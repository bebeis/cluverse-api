import http from 'k6/http';
import {check, sleep} from 'k6';
import {Counter, Rate, Trend} from 'k6/metrics';

// [TMP] 앞쪽 페이지(1~5)만 고정 조회 — OFFSET 영향 제거용 격리 테스트
// OFFSET이 아닌 다른 병목(커넥션 풀, JOIN, 인덱스 등)이 있는지 확인하기 위한 스크립트
//
// Example:
// k6 run \
//   -e BASE_URL=http://localhost:8080 \
//   -e BOARD_IDS=2001001,2001002,2000001 \
//   -e POPULAR_BOARD_ID=2001001 \
//   -e MAX_PAGE=5 \
//   -e RATE=600 \
//   -e DURATION=2m \
//   -e MAX_VUS=600 \
//   script/v1/k6/tmp-post-list-page1.k6.js

const BASE_URL           = __ENV.BASE_URL           || 'http://localhost:8080';
const MAX_PAGE           = Number(__ENV.MAX_PAGE     || 5);   // 앞쪽 페이지만
const SIZE               = 10;
const RATE               = Number(__ENV.RATE         || 100);
const DURATION           = __ENV.DURATION            || '1m';
const PRE_ALLOCATED_VUS  = Number(__ENV.PRE_ALLOCATED_VUS || 100);
const MAX_VUS            = Number(__ENV.MAX_VUS       || Math.max(PRE_ALLOCATED_VUS * 2, 200));
const THINK_TIME         = Number(__ENV.THINK_TIME   || 0);

// 게시판 가중치 (인기 게시판 7배)
const BOARD_WEIGHTS = (() => {
    const raw       = __ENV.BOARD_IDS   || '2001001,2001002,2000001';
    const popularId = Number(__ENV.POPULAR_BOARD_ID || 2001001);
    return raw.split(',').map(id => {
        const boardId = Number(id.trim());
        return {boardId, weight: boardId === popularId ? 7 : 1};
    });
})();

const BOARD_CUMULATIVE = (() => {
    let sum = 0;
    return BOARD_WEIGHTS.map(b => {
        sum += b.weight;
        return {boardId: b.boardId, cum: sum};
    });
})();
const BOARD_TOTAL_WEIGHT = BOARD_CUMULATIVE[BOARD_CUMULATIVE.length - 1].cum;

// 정렬: LATEST 80%, VIEW_COUNT 20%
const SORT_LATEST_RATIO = 0.8;

export const options = {
    scenarios: {
        post_list_page1: {
            executor: 'constant-arrival-rate',
            exec: 'readPostListScenario',
            rate: RATE,
            timeUnit: '1s',
            duration: DURATION,
            preAllocatedVUs: PRE_ALLOCATED_VUS,
            maxVUs: MAX_VUS,
        },
    },
    thresholds: {
        http_req_failed:      ['rate<0.01'],
        http_req_duration:    ['p(95)<1000', 'p(99)<2000'],
        post_list_duration:   ['p(95)<800',  'p(99)<1500'],
        post_list_success_rate: ['rate>0.99'],
    },
    summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
};

const postListDuration    = new Trend('post_list_duration', true);
const postListSuccessRate = new Rate('post_list_success_rate');
const postListRequests    = new Counter('post_list_requests');
const postListSelectedPage = new Trend('post_list_selected_page');

function pickBoard() {
    const r = Math.random() * BOARD_TOTAL_WEIGHT;
    for (const b of BOARD_CUMULATIVE) {
        if (r < b.cum) return b.boardId;
    }
    return BOARD_CUMULATIVE[BOARD_CUMULATIVE.length - 1].boardId;
}

function pickPage() {
    // 1 ~ MAX_PAGE 균등 랜덤 (OFFSET 영향 최소화)
    return Math.floor(Math.random() * MAX_PAGE) + 1;
}

function pickSort() {
    return Math.random() < SORT_LATEST_RATIO ? 'LATEST' : 'VIEW_COUNT';
}

function hasPostsArray(response) {
    try {
        return Array.isArray(response.json('data.posts'));
    } catch (_) {
        return false;
    }
}

export function readPostListScenario() {
    const boardId = pickBoard();
    const page    = pickPage();
    const sort    = pickSort();

    const res = http.get(
        `${BASE_URL}/api/v1/posts?boardId=${boardId}&sort=${sort}&page=${page}&size=${SIZE}`,
        {tags: {name: 'post_list_page1'}},
    );

    postListSelectedPage.add(page);
    postListDuration.add(res.timings.duration);

    const ok = check(res, {
        'post list returns 200':         (r) => r.status === 200,
        'post list returns posts array': (r) => hasPostsArray(r),
    });

    postListSuccessRate.add(ok);
    postListRequests.add(1);

    if (!ok) {
        console.error(`post list failed: status=${res.status}, board=${boardId}, sort=${sort}, page=${page}`);
    }

    if (THINK_TIME > 0) {
        sleep(THINK_TIME);
    }
}
