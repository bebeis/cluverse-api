// ---------------------------------------------------------------------------
// post-list-bench.k6.js — 게시글 목록 조회 V1~V3 공용 벤치마크
// ---------------------------------------------------------------------------
//
// 대상 API (offset 기반 페이지네이션 3세대):
//   V1  GET /api/v1/posts  — naive offset 풀 조인 + 전체 COUNT
//   V2  GET /api/v2/posts  — 커버링 인덱스 deferred join + 전체 COUNT
//   V3  GET /api/v3/posts  — V2 + 페이지 블록 상한 카운트 (page 1~500)
//
// 목적: 같은 트래픽 분포(traffic-profile)로 세 버전의 응답시간/처리량을 측정해
//       "offset 이 깊어질수록 V1 이 무너지고, V2 로 프로젝션 비용이 줄고,
//        V3 로 COUNT 비용이 상한된다"는 개선 서사를 실측한다.
//
// 두 가지 페이지 선택 모드:
//   PAGE_MODE=profile (기본) — traffic-profile 분포로 페이지를 샘플 (실사용 패턴)
//   PAGE_MODE=fixed + FIXED_PAGE=n — 특정 offset 구간만 반복 프로브
//        (offset 곡선 그래프용. size=20 기준 page 1/5000/20000 = offset 0/10만/40만)
//
// [실행 예시]
//   # 1) 프로파일 모드 — V2 를 실사용 분포로 (기본 모드)
//   k6 run -e VERSION=v2 -e BOARD_ID=2001001 -e RATE=200 -e DURATION=2m \
//          script/post-list/k6/post-list-bench.k6.js
//
//   # 2) 고정 페이지 모드 — V1 을 offset 40만(page 20000) 지점만 프로브
//   k6 run -e VERSION=v1 -e BOARD_ID=2001001 -e PAGE_MODE=fixed -e FIXED_PAGE=20000 \
//          -e RATE=50 -e DURATION=1m script/post-list/k6/post-list-bench.k6.js
//
//   # 3) 버전만 바꿔가며 동일 조건 비교 (VERSION 만 교체해서 3회 실행)
//   for v in v1 v2 v3; do \
//     k6 run -e VERSION=$v -e BOARD_ID=2001001 -e RATE=200 -e DURATION=2m \
//            script/post-list/k6/post-list-bench.k6.js; done
//
// [주의] 기본 BOARD_ID 는 핫보드 2001001 (05a: post_id 5000001~6000000 100만건,
//        05b 누적 700만). 깊은 offset / 100만+ 서사는 이 보드에서만 재현된다.
//        저밀도 일반 보드로 비교하려면 -e BOARD_ID=2000001 (2M 을 120개 보드에 분산
//        → 보드당 ~1.6만건). 자세한 시드 범위는 script/post-list/README.md 참고.
// ---------------------------------------------------------------------------

import http from 'k6/http';
import { check } from 'k6';
import { Rate, Trend } from 'k6/metrics';
import { pickPage } from './lib/traffic-profile.js';

const VERSION = (__ENV.VERSION || '').toLowerCase();
if (!['v1', 'v2', 'v3'].includes(VERSION)) {
    throw new Error(`VERSION 은 v1|v2|v3 중 하나여야 합니다 (현재: "${__ENV.VERSION}")`);
}

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const BOARD_ID = Number(__ENV.BOARD_ID || 2001001);
const SIZE = Number(__ENV.SIZE || 20);
const SORT = __ENV.SORT; // LATEST | VIEW_COUNT (미지정 시 서버 기본)
const CATEGORY = __ENV.CATEGORY;
const DATE = __ENV.DATE; // V3 전용 옵션 (yyyy-MM-dd)

// V3 는 page 상한이 500, V1/V2 는 20000
const DEFAULT_MAX_PAGE = VERSION === 'v3' ? 500 : 20000;
const MAX_PAGE = Number(__ENV.MAX_PAGE || DEFAULT_MAX_PAGE);

const PAGE_MODE = (__ENV.PAGE_MODE || 'profile').toLowerCase();
const FIXED_PAGE = Number(__ENV.FIXED_PAGE || 1);

const RATE = Number(__ENV.RATE || 100);
const DURATION = __ENV.DURATION || '1m';
const PRE_ALLOCATED_VUS = Number(__ENV.PRE_ALLOCATED_VUS || 50);
const MAX_VUS = Number(__ENV.MAX_VUS || Math.max(PRE_ALLOCATED_VUS * 2, 100));

export const options = {
    scenarios: {
        post_list_bench: {
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
    thresholds: {
        http_req_failed: ['rate<0.05'],
    },
    summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
    tags: {
        version: VERSION,
        page_mode: PAGE_MODE,
    },
};

// 커스텀 메트릭
const listDuration = new Trend('post_list_duration', true);
const listSuccessRate = new Rate('post_list_success_rate');
const selectedPage = new Trend('post_list_selected_page');

function nextPage() {
    if (PAGE_MODE === 'fixed') {
        return Math.min(Math.max(1, FIXED_PAGE), MAX_PAGE);
    }
    return pickPage(MAX_PAGE);
}

function buildUrl(page) {
    const params = [
        `boardId=${encodeURIComponent(String(BOARD_ID))}`,
        `page=${encodeURIComponent(String(page))}`,
        `size=${encodeURIComponent(String(SIZE))}`,
    ];
    if (SORT) params.push(`sort=${encodeURIComponent(SORT)}`);
    if (CATEGORY) params.push(`category=${encodeURIComponent(CATEGORY)}`);
    if (VERSION === 'v3' && DATE) params.push(`date=${encodeURIComponent(DATE)}`);
    return `${BASE_URL}/api/${VERSION}/posts?${params.join('&')}`;
}

function hasPostsArray(response) {
    try {
        return Array.isArray(response.json('data.posts'));
    } catch (_err) {
        return false;
    }
}

export function benchScenario() {
    const page = nextPage();
    const response = http.get(buildUrl(page), {
        tags: { name: 'post_list_bench' },
    });

    selectedPage.add(page);
    listDuration.add(response.timings.duration);

    const ok = check(response, {
        'status is 200': (res) => res.status === 200,
        'has posts array': (res) => hasPostsArray(res),
    });

    listSuccessRate.add(ok);

    if (!ok) {
        console.error(
            `[${VERSION}] page=${page} 실패: status=${response.status}, body=${String(response.body).slice(0, 300)}`,
        );
    }
}
