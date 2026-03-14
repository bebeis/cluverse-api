import http from 'k6/http';
import {check, sleep} from 'k6';
import {Counter, Rate, Trend} from 'k6/metrics';

// Example:
// k6 run \
//   -e BASE_URL=http://localhost:8080 \
//   -e BOARD_ID=2000001 \
//   -e RATE=100 \
//   -e DURATION=2m \
//   script/v1/k6/post-list-read-page1.k6.js

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const BOARD_ID = Number(__ENV.BOARD_ID || 2000001);
const CATEGORY = __ENV.CATEGORY;
const SORT = __ENV.SORT || 'LATEST';
const SIZE = 10;
const RATE = Number(__ENV.RATE || 50);
const DURATION = __ENV.DURATION || '1m';
const PRE_ALLOCATED_VUS = Number(__ENV.PRE_ALLOCATED_VUS || 50);
const MAX_VUS = Number(__ENV.MAX_VUS || Math.max(PRE_ALLOCATED_VUS * 2, 100));
const THINK_TIME = Number(__ENV.THINK_TIME || 0);

export const options = {
    scenarios: {
        post_list_read_page1: {
            executor: 'constant-arrival-rate',
            exec: 'readPostListPage1Scenario',
            rate: RATE,
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
        post_list_success_rate: ['rate>0.99'],
    },
    summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
};

const postListDuration = new Trend('post_list_duration', true);
const postListSuccessRate = new Rate('post_list_success_rate');
const postListRequests = new Counter('post_list_requests');

function buildListUrl() {
    const params = [
        `boardId=${encodeURIComponent(String(BOARD_ID))}`,
        `sort=${encodeURIComponent(SORT)}`,
        `page=1`,
        `size=${encodeURIComponent(String(SIZE))}`,
    ];

    if (CATEGORY) {
        params.push(`category=${encodeURIComponent(CATEGORY)}`);
    }

    return `${BASE_URL}/api/v1/posts?${params.join('&')}`;
}

function hasPostsArray(response) {
    try {
        return Array.isArray(response.json('data.posts'));
    } catch (_error) {
        return false;
    }
}

export function readPostListPage1Scenario() {
    const url = buildListUrl();
    const response = http.get(url, {
        tags: {
            name: 'post_list_read_page1',
        },
    });

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
