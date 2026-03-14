import http from 'k6/http';
import {check, fail, sleep} from 'k6';
import exec from 'k6/execution';
import {Counter, Rate, Trend} from 'k6/metrics';

// Example:
// LOGIN_EMAIL=seed_user_000101@seed.local LOGIN_PASSWORD=... \
// BOARD_ID=2000001 RATE=50 DURATION=2m \
// k6 run script/v1/k6/post-create.k6.js

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const LOGIN_EMAIL = __ENV.LOGIN_EMAIL;
const LOGIN_PASSWORD = __ENV.LOGIN_PASSWORD;
const BOARD_ID = Number(__ENV.BOARD_ID || 2000001);
const CATEGORY = __ENV.CATEGORY || 'INFORMATION';
const IS_ANONYMOUS = String(__ENV.IS_ANONYMOUS || 'false').toLowerCase() === 'true';
const IS_EXTERNAL_VISIBLE = String(__ENV.IS_EXTERNAL_VISIBLE || 'true').toLowerCase() === 'true';
const RATE = Number(__ENV.RATE || 20);
const DURATION = __ENV.DURATION || '1m';
const PRE_ALLOCATED_VUS = Number(__ENV.PRE_ALLOCATED_VUS || 20);
const MAX_VUS = Number(__ENV.MAX_VUS || Math.max(PRE_ALLOCATED_VUS * 2, 40));
const THINK_TIME = Number(__ENV.THINK_TIME || 0);

if (!LOGIN_EMAIL || !LOGIN_PASSWORD) {
    throw new Error('LOGIN_EMAIL and LOGIN_PASSWORD must be provided.');
}

export const options = {
    scenarios: {
        post_create: {
            executor: 'constant-arrival-rate',
            exec: 'createPostScenario',
            rate: RATE,
            timeUnit: '1s',
            duration: DURATION,
            preAllocatedVUs: PRE_ALLOCATED_VUS,
            maxVUs: MAX_VUS,
        },
    },
    thresholds: {
        http_req_failed: ['rate<0.01'],
        http_req_duration: ['p(95)<1500', 'p(99)<3000'],
        post_create_duration: ['p(95)<1000', 'p(99)<2000'],
        post_create_success_rate: ['rate>0.99'],
    },
    summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
};

const postCreateDuration = new Trend('post_create_duration', true);
const postCreateSuccessRate = new Rate('post_create_success_rate');
const postsCreated = new Counter('posts_created');

let sessionCookie;

function extractPostId(response) {
    try {
        const postId = response.json('data.postId');
        return typeof postId === 'number' ? postId : null;
    } catch (_error) {
        return null;
    }
}

function login() {
    if (sessionCookie) {
        return sessionCookie;
    }

    const response = http.post(
        `${BASE_URL}/api/v1/auth/login`,
        JSON.stringify({
            email: LOGIN_EMAIL,
            password: LOGIN_PASSWORD,
        }),
        {
            headers: {
                'Content-Type': 'application/json',
            },
            tags: {
                name: 'auth_login',
            },
        },
    );

    const loginSucceeded = check(response, {
        'login returns 200': (res) => res.status === 200,
        'login returns JSESSIONID': (res) => Boolean(res.cookies.JSESSIONID && res.cookies.JSESSIONID.length > 0),
    });

    if (!loginSucceeded) {
        fail(`login failed: status=${response.status}, body=${response.body}`);
    }

    sessionCookie = `JSESSIONID=${response.cookies.JSESSIONID[0].value}`;
    return sessionCookie;
}

function buildPostPayload() {
    const uniqueSuffix = `${exec.vu.idInTest}-${exec.scenario.iterationInTest}-${Date.now()}`;

    return JSON.stringify({
        boardId: BOARD_ID,
        title: `k6-post-create-${uniqueSuffix}`,
        content: `k6 post create performance test payload ${uniqueSuffix}`,
        category: CATEGORY,
        tags: ['k6', 'perf'],
        isAnonymous: IS_ANONYMOUS,
        isPinned: false,
        isExternalVisible: IS_EXTERNAL_VISIBLE,
        imageUrls: [],
    });
}

export function createPostScenario() {
    const response = http.post(
        `${BASE_URL}/api/v1/posts`,
        buildPostPayload(),
        {
            headers: {
                'Content-Type': 'application/json',
                Cookie: login(),
            },
            tags: {
                name: 'post_create',
            },
        },
    );

    postCreateDuration.add(response.timings.duration);

    const created = check(response, {
        'create post returns 201': (res) => res.status === 201,
        'create post returns postId': (res) => {
            const postId = extractPostId(res);
            return postId !== null && postId > 0;
        },
    });

    postCreateSuccessRate.add(created);

    if (created) {
        postsCreated.add(1);
    } else if (exec.scenario.iterationInTest < 5) {
        console.error(`post create failed: status=${response.status}, body=${response.body}`);
    }

    if (THINK_TIME > 0) {
        sleep(THINK_TIME);
    }
}
