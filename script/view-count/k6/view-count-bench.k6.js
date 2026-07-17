// ---------------------------------------------------------------------------
// view-count-bench.k6.js — 게시글 조회수 증가 V1~V3 공용 벤치마크
// ---------------------------------------------------------------------------
//
// 대상 API (동시성 제어 전략 3종):
//   V1  POST /api/v1/posts/{postId}/view-count  — 낙관적 락 (@Version + 재시도)
//   V2  POST /api/v2/posts/{postId}/view-count  — 비관적 락 (select for update)
//   V3  POST /api/v3/posts/{postId}/view-count  — 원자적 UPDATE (운영 방식)
//
// 목적: 같은 조건으로 세 버전의 응답시간/처리량을 측정해
//       "핫 레코드 경합에서 락 보유 시간이 짧을수록 응답이 빨라진다"는 서사를 실측한다.
//
// 두 가지 게시글 선택 모드:
//   POST_MODE=fixed (기본) + POST_ID=n — 단일 게시글에 쓰기 집중 (핫 레코드 경합.
//        락 전략 비교의 핵심 시나리오. RATE 를 올려가며 경합 강도를 조절한다)
//   POST_MODE=profile — traffic-profile 분포로 게시글을 샘플 (현실 트래픽:
//        최신글 집중 + 롱테일. 분산 쓰기 상황에서의 평균 그림)
//
// [실행 예시]
//   # 1) 고정 모드 — 단일 핫 레코드에 세 버전을 같은 조건으로 (핵심 측정)
//   for v in v1 v2 v3; do \
//     k6 run -e VERSION=$v -e RATE=300 -e DURATION=1m \
//            script/view-count/k6/view-count-bench.k6.js; done
//
//   # 2) 프로파일 모드 — 현실 분포 분산 쓰기
//   k6 run -e VERSION=v3 -e POST_MODE=profile -e RATE=300 -e DURATION=1m \
//          script/view-count/k6/view-count-bench.k6.js
//
// [주의] 기본 POST_ID=6000000 (05a 시드의 핫보드 2001001 최신 글).
//        profile 모드 기본 범위는 05a(5000001~6000000). 05b까지 넣었다면
//        -e POST_ID_MAX=13999999 로 범위를 넓힐 수 있다.
//        V1(낙관) 측정 전 05c 시드(post_view_count_optimistic 사전 적재) 필수.
// ---------------------------------------------------------------------------

import http from 'k6/http';
import { check } from 'k6';
import { Rate, Trend } from 'k6/metrics';
import { pickPostId } from './lib/traffic-profile.js';

const VERSION = (__ENV.VERSION || '').toLowerCase();
if (!['v1', 'v2', 'v3'].includes(VERSION)) {
    throw new Error(`VERSION 은 v1|v2|v3 중 하나여야 합니다 (현재: "${__ENV.VERSION}")`);
}

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

const POST_MODE = (__ENV.POST_MODE || 'fixed').toLowerCase();
if (!['fixed', 'profile'].includes(POST_MODE)) {
    throw new Error(`POST_MODE 는 fixed|profile 중 하나여야 합니다 (현재: "${__ENV.POST_MODE}")`);
}

// fixed 모드 대상 게시글 (기본 = 05a 핫보드의 최신 글)
const POST_ID = Number(__ENV.POST_ID || 6000000);
// profile 모드 게시글 범위 (기본 = 05a: 5000001~6000000)
const POST_ID_MIN = Number(__ENV.POST_ID_MIN || 5000001);
const POST_ID_MAX = Number(__ENV.POST_ID_MAX || 6000000);

const RATE = Number(__ENV.RATE || 100);
const DURATION = __ENV.DURATION || '1m';
const PRE_ALLOCATED_VUS = Number(__ENV.PRE_ALLOCATED_VUS || 50);
const MAX_VUS = Number(__ENV.MAX_VUS || Math.max(PRE_ALLOCATED_VUS * 2, 100));

export const options = {
    scenarios: {
        view_count_bench: {
            executor: 'constant-arrival-rate',
            exec: 'benchScenario',
            rate: RATE,
            timeUnit: '1s',
            duration: DURATION,
            preAllocatedVUs: PRE_ALLOCATED_VUS,
            maxVUs: MAX_VUS,
        },
    },
    // 측정이 목적이므로 threshold 는 관대하게(실패 처리보다 기록 위주). 에러율만 가드.
    // 단 V1(낙관)은 재시도 소진 500 이 관찰 대상이므로, 이 가드에 걸리면 그것 자체가 결과다.
    thresholds: {
        http_req_failed: ['rate<0.05'],
    },
    summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
    tags: {
        version: VERSION,
        post_mode: POST_MODE,
    },
};

// 커스텀 메트릭
const viewCountDuration = new Trend('view_count_duration', true);
const viewCountSuccessRate = new Rate('view_count_success_rate');
// V1(낙관적 락) 전용 관찰: 재시도 10회 소진 시 서버가 500 을 반환한다.
// V2/V3 에서는 항상 0 이어야 정상.
const retryExhaustedRate = new Rate('view_count_retry_exhausted');

function nextPostId() {
    if (POST_MODE === 'fixed') {
        return POST_ID;
    }
    return pickPostId(POST_ID_MIN, POST_ID_MAX);
}

export function benchScenario() {
    const postId = nextPostId();
    const response = http.post(
        `${BASE_URL}/api/${VERSION}/posts/${postId}/view-count`,
        null,
        { tags: { name: 'view_count_bench' } },
    );

    viewCountDuration.add(response.timings.duration);
    retryExhaustedRate.add(response.status === 500);

    const ok = check(response, {
        'status is 200': (res) => res.status === 200,
    });

    viewCountSuccessRate.add(ok);

    if (!ok && response.status !== 500) {
        console.error(
            `[${VERSION}] postId=${postId} 실패: status=${response.status}, body=${String(response.body).slice(0, 300)}`,
        );
    }
}
