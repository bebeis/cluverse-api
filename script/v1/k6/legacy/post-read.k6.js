import http from 'k6/http';
import {check, sleep} from 'k6';
import {Counter, Rate, Trend} from 'k6/metrics';

// Example:
// k6 run \
//   -e BASE_URL=http://localhost:8080 \
//   -e BOARD_ID=2000001 \
//   -e POST_ID=3000001 \
//   -e MAX_PAGE=10000 \
//   -e PAGE_BIAS=3 \
//   -e LIST_RATE=100 \
//   -e DETAIL_RATE=100 \
//   -e DURATION=2m \
//   script/v1/k6/post-read.k6.js

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const BOARD_ID = Number(__ENV.BOARD_ID || 2000001);
const POST_ID = Number(__ENV.POST_ID || 3000001);
const CATEGORY = __ENV.CATEGORY;
const SORT = __ENV.SORT || 'LATEST';
const MAX_PAGE = Number(__ENV.MAX_PAGE || 1000);
const PAGE_BIAS = Number(__ENV.PAGE_BIAS || 3);
const SIZE = 10;
const LIST_RATE = Number(__ENV.LIST_RATE || 50);
const DETAIL_RATE = Number(__ENV.DETAIL_RATE || 50);
const DURATION = __ENV.DURATION || '1m';
const PRE_ALLOCATED_VUS = Number(__ENV.PRE_ALLOCATED_VUS || 50);
const MAX_VUS = Number(__ENV.MAX_VUS || Math.max(PRE_ALLOCATED_VUS * 2, 100));
const THINK_TIME = Number(__ENV.THINK_TIME || 0);

export const options = {
    scenarios: {
        post_list_read: {
            executor: 'constant-arrival-rate',
            exec: 'readPostListScenario',
            rate: LIST_RATE,
            timeUnit: '1s',
            duration: DURATION,
            preAllocatedVUs: PRE_ALLOCATED_VUS,
            maxVUs: MAX_VUS,
        },
        post_detail_read: {
            executor: 'constant-arrival-rate',
            exec: 'readPostDetailScenario',
            rate: DETAIL_RATE,
            timeUnit: '1s',
            duration: DURATION,
            preAllocatedVUs: PRE_ALLOCATED_VUS,
            maxVUs: MAX_VUS,
        },
    },
    thresholds: {
        http_req_failed: ['rate<0.01'],
        http_req_duration: ['p(95)<1000', 'p(99)<2000'],
        post_list_duration: ['p(95)<800', 'p(99)<1500'],
        post_detail_duration: ['p(95)<800', 'p(99)<1500'],
        post_list_success_rate: ['rate>0.99'],
        post_detail_success_rate: ['rate>0.99'],
    },
    summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
};

const postListDuration = new Trend('post_list_duration', true);
const postDetailDuration = new Trend('post_detail_duration', true);
const postListSuccessRate = new Rate('post_list_success_rate');
const postDetailSuccessRate = new Rate('post_detail_success_rate');
const postListRequests = new Counter('post_list_requests');
const postDetailRequests = new Counter('post_detail_requests');
const postListSelectedPage = new Trend('post_list_selected_page');

function pickPage() {
    const maxPage = Math.max(1, Math.floor(MAX_PAGE));
    const bias = PAGE_BIAS > 0 ? PAGE_BIAS : 3;

    return Math.min(
        maxPage,
        Math.floor(Math.pow(Math.random(), bias) * maxPage) + 1,
    );
}

function buildListUrl(page) {
    const params = [
        `boardId=${encodeURIComponent(String(BOARD_ID))}`,
        `sort=${encodeURIComponent(SORT)}`,
        `page=${encodeURIComponent(String(page))}`,
        `size=${encodeURIComponent(String(SIZE))}`,
    ];

    if (CATEGORY) {
        params.push(`category=${encodeURIComponent(CATEGORY)}`);
    }

    return `${BASE_URL}/api/v1/posts?${params.join('&')}`;
}

function buildDetailUrl() {
    return `${BASE_URL}/api/v1/posts/${POST_ID}`;
}

function hasPostsArray(response) {
    try {
        return Array.isArray(response.json('data.posts'));
    } catch (_error) {
        return false;
    }
}

function hasPostId(response) {
    try {
        const postId = response.json('data.postId');
        return typeof postId === 'number' && postId > 0;
    } catch (_error) {
        return false;
    }
}

export function readPostListScenario() {
    const page = pickPage();
    const response = http.get(buildListUrl(page), {
        tags: {
            name: 'post_list_read',
        },
    });

    postListSelectedPage.add(page);
    postListDuration.add(response.timings.duration);

    const ok = check(response, {
        'post list returns 200': (res) => res.status === 200,
        'post list returns posts array': (res) => hasPostsArray(res),
    });

    postListSuccessRate.add(ok);
    postListRequests.add(1);

    if (!ok) {
        console.error(`post list failed: status=${response.status}, body=${response.body}`);
    }

    if (THINK_TIME > 0) {
        sleep(THINK_TIME);
    }
}

export function readPostDetailScenario() {
    const response = http.get(buildDetailUrl(), {
        tags: {
            name: 'post_detail_read',
        },
    });

    postDetailDuration.add(response.timings.duration);

    const ok = check(response, {
        'post detail returns 200': (res) => res.status === 200,
        'post detail returns postId': (res) => hasPostId(res),
    });

    postDetailSuccessRate.add(ok);
    postDetailRequests.add(1);

    if (!ok) {
        console.error(`post detail failed: status=${response.status}, body=${response.body}`);
    }

    if (THINK_TIME > 0) {
        sleep(THINK_TIME);
    }
}
