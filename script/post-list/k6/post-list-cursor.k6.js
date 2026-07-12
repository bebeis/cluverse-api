// ---------------------------------------------------------------------------
// post-list-cursor.k6.js — 게시글 목록 조회 V4 (날짜 앵커 + 커서) 세션 플로우
// ---------------------------------------------------------------------------
//
// 대상 API:
//   V4  GET /api/v4/posts  — 날짜 앵커 + 튜플 커서 (offset 없음)
//   응답 data: { posts, size, hasNext, hasPrev,
//                prevCursor:{createdAt, postId}, nextCursor:{createdAt, postId} }
//
// 목적: 커서 페이지네이션은 "몇 페이지째냐"와 무관하게 매 이동이 O(1) 이라는 것을
//       실측한다. 그래서 offset 벤치(post-list-bench)와 달리 단일 요청이 아니라
//       "세션"을 측정 단위로 삼는다 — 진입 후 nextCursor 를 따라 여러 번 NEXT 이동.
//
// [세션 구조]  (iteration 하나 = 세션 하나)
//   1) 진입: 무앵커 최신 조회 (또는 -e DATE=yyyy-MM-dd 로 특정 날짜 앵커 진입)
//   2) 응답의 nextCursor 로 NEXT 이동을 depth 회 반복
//      depth 는 traffic-profile 의 페이지 분포에서 샘플 (offset 벤치와 동일 분포
//      재사용 → 공정 비교). 단 순차 이동이므로 CURSOR_MAX_DEPTH 로 상한한다.
//   3) hasNext=false 를 만나면 조기 종료
//
// [핵심 계측] 이동 스텝별 응답시간 Trend 를 depth 구간(bucket) 태그로 나눠 기록한다.
//            깊은 스텝(수십 번째 이동)도 첫 스텝과 지연이 같음 = O(1) 을 보이는 게 목적.
//
// [실행 예시]
//   # 무앵커 최신 진입 → 커서 워크
//   k6 run -e BOARD_ID=2001001 -e RATE=100 -e DURATION=2m \
//          script/post-list/k6/post-list-cursor.k6.js
//
//   # 특정 날짜 앵커로 진입
//   k6 run -e BOARD_ID=2001001 -e DATE=2024-06-01 -e RATE=100 -e DURATION=2m \
//          script/post-list/k6/post-list-cursor.k6.js
//
// [주의] 기본 BOARD_ID 는 핫보드 2001001 (100만+). 저밀도 비교는 -e BOARD_ID=2000001 (README 참고).
// ---------------------------------------------------------------------------

import http from 'k6/http';
import { check } from 'k6';
import { Rate, Trend } from 'k6/metrics';
import { pickPage } from './lib/traffic-profile.js';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const BOARD_ID = Number(__ENV.BOARD_ID || 2001001);
const SIZE = Number(__ENV.SIZE || 20);
const CATEGORY = __ENV.CATEGORY;
const DATE = __ENV.DATE; // yyyy-MM-dd, 무앵커 최신이면 미지정

// 커서 워크 최대 깊이. traffic-profile 분포를 이 상한으로 클램프해 depth 를 뽑는다.
// (offset 벤치의 MAX_PAGE 는 20000 이지만, 커서는 순차 이동이라 세션당 요청 폭발을
//  막기 위해 별도 상한을 둔다. 분포 "모양"은 동일하게 앞쪽 편중을 유지한다.)
const CURSOR_MAX_DEPTH = Number(__ENV.CURSOR_MAX_DEPTH || 100);

const RATE = Number(__ENV.RATE || 50); // 세션 시작률 (sessions/s)
const DURATION = __ENV.DURATION || '1m';
const PRE_ALLOCATED_VUS = Number(__ENV.PRE_ALLOCATED_VUS || 50);
const MAX_VUS = Number(__ENV.MAX_VUS || Math.max(PRE_ALLOCATED_VUS * 2, 100));

export const options = {
    scenarios: {
        // 세션을 일정 비율로 시작한다. 각 iteration 이 진입+커서워크 전체를 순차 수행.
        cursor_sessions: {
            executor: 'constant-arrival-rate',
            exec: 'cursorSession',
            rate: RATE,
            timeUnit: '1s',
            duration: DURATION,
            preAllocatedVUs: PRE_ALLOCATED_VUS,
            maxVUs: MAX_VUS,
        },
    },
    thresholds: {
        http_req_failed: ['rate<0.05'],
    },
    summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
    tags: {
        version: 'v4',
    },
};

// 커스텀 메트릭
const entryDuration = new Trend('cursor_entry_duration', true); // 진입 요청
const stepDuration = new Trend('cursor_step_duration', true); // 모든 NEXT 이동 통합
const sessionSuccessRate = new Rate('cursor_session_success_rate');
const sessionDepth = new Trend('cursor_session_depth'); // 세션이 실제 이동한 스텝 수

// 스텝 인덱스를 구간으로 묶어 태그를 만든다 → "깊이별 지연이 평평한가" 확인용
function depthBucket(step) {
    if (step <= 1) return 'd01';
    if (step <= 5) return 'd02-05';
    if (step <= 10) return 'd06-10';
    if (step <= 25) return 'd11-25';
    if (step <= 50) return 'd26-50';
    return 'd51+';
}

function buildEntryUrl() {
    const params = [
        `boardId=${encodeURIComponent(String(BOARD_ID))}`,
        `size=${encodeURIComponent(String(SIZE))}`,
    ];
    if (CATEGORY) params.push(`category=${encodeURIComponent(CATEGORY)}`);
    if (DATE) params.push(`date=${encodeURIComponent(DATE)}`);
    return `${BASE_URL}/api/v4/posts?${params.join('&')}`;
}

function buildNextUrl(cursor) {
    const params = [
        `boardId=${encodeURIComponent(String(BOARD_ID))}`,
        `size=${encodeURIComponent(String(SIZE))}`,
        `direction=NEXT`,
        `cursorCreatedAt=${encodeURIComponent(cursor.createdAt)}`,
        `cursorPostId=${encodeURIComponent(String(cursor.postId))}`,
    ];
    if (CATEGORY) params.push(`category=${encodeURIComponent(CATEGORY)}`);
    return `${BASE_URL}/api/v4/posts?${params.join('&')}`;
}

// 응답에서 data 객체를 안전하게 추출
function readData(response) {
    try {
        return response.json('data');
    } catch (_err) {
        return null;
    }
}

export function cursorSession() {
    // depth 를 traffic-profile 분포(앞쪽 편중)에서 샘플, CURSOR_MAX_DEPTH 로 상한
    const targetDepth = pickPage(CURSOR_MAX_DEPTH);

    // 1) 진입
    const entry = http.get(buildEntryUrl(), { tags: { name: 'cursor_entry' } });
    entryDuration.add(entry.timings.duration);

    let data = readData(entry);
    const entryOk = check(entry, {
        'entry status 200': (res) => res.status === 200,
        'entry has posts array': () => data != null && Array.isArray(data.posts),
    });
    if (!entryOk) {
        sessionSuccessRate.add(false);
        console.error(`[v4-entry] 실패: status=${entry.status}, body=${String(entry.body).slice(0, 300)}`);
        return;
    }

    // 2) nextCursor 를 따라 NEXT 이동 반복
    let moved = 0;
    let allOk = true;
    for (let step = 1; step < targetDepth; step += 1) {
        if (!data.hasNext || !data.nextCursor) break;

        const res = http.get(buildNextUrl(data.nextCursor), {
            tags: { name: 'cursor_next', depth: depthBucket(step) },
        });
        stepDuration.add(res.timings.duration, { depth: depthBucket(step) });

        data = readData(res);
        const stepOk = check(res, {
            'next status 200': (r) => r.status === 200,
            'next has posts array': () => data != null && Array.isArray(data.posts),
        });
        if (!stepOk) {
            allOk = false;
            console.error(`[v4-next] step=${step} 실패: status=${res.status}, body=${String(res.body).slice(0, 300)}`);
            break;
        }
        moved += 1;
    }

    sessionDepth.add(moved);
    sessionSuccessRate.add(allOk);
}
