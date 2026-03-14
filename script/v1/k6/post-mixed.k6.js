import http from 'k6/http';
import {check, sleep} from 'k6';
import exec from 'k6/execution';
import {Counter, Rate, Trend} from 'k6/metrics';

// Example:
// k6 run \
//   -e BASE_URL=http://localhost:8080 \
//   -e LOGIN_PASSWORD=seed12345 \
//   -e BOARD_IDS=2001001,2001002,2000001 \
//   -e POPULAR_BOARD_ID=2001001 \
//   -e WRITE_BOARD_ID=2001001 \
//   -e MAX_PAGE=500 \
//   -e RATE=50 \
//   -e DURATION=2m \
//   script/v1/k6/post-mixed.k6.js
//
// 반영된 실제 트래픽 패턴:
//   - 읽기 90% / 쓰기 10% 혼합
//   - 읽기 중 페이지 기반 85% / 날짜 기반 15% (500페이지 이상은 날짜 기반 유도)
//   - 여러 게시판을 가중치 기반으로 랜덤 선택 (인기 게시판에 집중)
//   - LATEST / VIEW_COUNT 정렬 혼합 (80% / 20%)
//   - 앞 페이지 편향 랜덤 페이지 선택 (최대 500페이지)
//   - 날짜 기반 조회: 최근 2년 이내 랜덤 날짜
//   - 쓰기는 VU별 세션 유지 (1회 로그인 후 재사용)

const BASE_URL        = __ENV.BASE_URL        || 'http://localhost:8080';
const LOGIN_PASSWORD  = __ENV.LOGIN_PASSWORD  || 'seed12345';
const WRITE_BOARD_ID  = Number(__ENV.WRITE_BOARD_ID  || 2001001);
const MAX_PAGE        = Number(__ENV.MAX_PAGE  || 500);
const PAGE_BIAS       = Number(__ENV.PAGE_BIAS || 3);
// 읽기 중 날짜 기반 조회 비율 (15%)
const DATE_READ_RATIO = Number(__ENV.DATE_READ_RATIO || 0.15);
const SIZE            = 10;
const RATE            = Number(__ENV.RATE      || 50);
const DURATION        = __ENV.DURATION         || '1m';
const PRE_ALLOCATED_VUS = Number(__ENV.PRE_ALLOCATED_VUS || 50);
const MAX_VUS         = Number(__ENV.MAX_VUS   || Math.max(PRE_ALLOCATED_VUS * 2, 100));
const THINK_TIME      = Number(__ENV.THINK_TIME || 0);

// 읽기 90% / 쓰기 10%
const WRITE_RATIO = 0.1;

// 게시판 목록: [board_id, 상대 가중치]
const BOARD_WEIGHTS = (() => {
    const raw = __ENV.BOARD_IDS || '2001001,2001002,2000001';
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

// 정렬 가중치: LATEST 80%, VIEW_COUNT 20%
const SORT_LATEST_RATIO = 0.8;

// 시드 유저 범위
const SEED_USER_START = 1;
const SEED_USER_END   = 50000;

export const options = {
    scenarios: {
        post_mixed: {
            executor: 'constant-arrival-rate',
            exec: 'postMixedScenario',
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
        post_list_duration: ['p(95)<800', 'p(99)<1500'],
        post_list_date_duration: ['p(95)<500', 'p(99)<1000'],
        post_create_duration: ['p(95)<1000', 'p(99)<2000'],
        post_list_success_rate: ['rate>0.99'],
        post_list_date_success_rate: ['rate>0.99'],
        post_create_success_rate: ['rate>0.99'],
    },
    summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
};

const postListDuration      = new Trend('post_list_duration', true);
const postListDateDuration  = new Trend('post_list_date_duration', true);
const postCreateDuration    = new Trend('post_create_duration', true);
const postListSuccessRate   = new Rate('post_list_success_rate');
const postListDateSuccessRate = new Rate('post_list_date_success_rate');
const postCreateSuccessRate = new Rate('post_create_success_rate');
const postListRequests      = new Counter('post_list_requests');
const postListDateRequests  = new Counter('post_list_date_requests');
const postCreateRequests    = new Counter('post_create_requests');
const postListSelectedPage  = new Trend('post_list_selected_page');

// VU별 세션 캐시
const sessionCache = {};

function pickBoard() {
    const r = Math.random() * BOARD_TOTAL_WEIGHT;
    for (const b of BOARD_CUMULATIVE) {
        if (r < b.cum) return b.boardId;
    }
    return BOARD_CUMULATIVE[BOARD_CUMULATIVE.length - 1].boardId;
}

function pickPage() {
    const maxPage = Math.max(1, Math.floor(MAX_PAGE));
    const bias = PAGE_BIAS > 0 ? PAGE_BIAS : 3;
    return Math.min(maxPage, Math.floor(Math.pow(Math.random(), bias) * maxPage) + 1);
}

function pickSort() {
    return Math.random() < SORT_LATEST_RATIO ? 'LATEST' : 'VIEW_COUNT';
}

function seedEmail(vuId) {
    const idx = ((vuId - 1) % (SEED_USER_END - SEED_USER_START + 1)) + SEED_USER_START;
    return `seed_user_${String(idx).padStart(6, '0')}@seed.local`;
}

function login(vuId) {
    if (sessionCache[vuId]) {
        return sessionCache[vuId];
    }

    const email = seedEmail(vuId);
    const res = http.post(
        `${BASE_URL}/api/v1/auth/login`,
        JSON.stringify({email, password: LOGIN_PASSWORD}),
        {
            headers: {'Content-Type': 'application/json'},
            tags: {name: 'auth_login'},
        },
    );

    const ok = check(res, {
        'login returns 200': (r) => r.status === 200,
        'login returns JSESSIONID': (r) => Boolean(r.cookies.JSESSIONID && r.cookies.JSESSIONID.length > 0),
    });

    if (!ok) {
        console.warn(`login failed for ${email}: status=${res.status}`);
        return null;
    }

    sessionCache[vuId] = `JSESSIONID=${res.cookies.JSESSIONID[0].value}`;
    return sessionCache[vuId];
}

function hasPostsArray(response) {
    try {
        return Array.isArray(response.json('data.posts'));
    } catch (_) {
        return false;
    }
}

function pickDate() {
    // 최근 2년 이내 랜덤 날짜 (YYYY-MM-DD)
    const now = new Date();
    const msPerDay = 24 * 60 * 60 * 1000;
    const daysRange = 365 * 2;
    const randomMs = Math.floor(Math.random() * daysRange) * msPerDay;
    const target = new Date(now.getTime() - randomMs);
    return target.toISOString().slice(0, 10);
}

function readPostList() {
    const boardId = pickBoard();
    const page    = pickPage();
    const sort    = pickSort();

    const res = http.get(
        `${BASE_URL}/api/v1/posts?boardId=${boardId}&sort=${sort}&page=${page}&size=${SIZE}`,
        {tags: {name: 'post_list'}},
    );

    postListSelectedPage.add(page);
    postListDuration.add(res.timings.duration);

    const ok = check(res, {
        'post list returns 200': (r) => r.status === 200,
        'post list returns posts array': (r) => hasPostsArray(r),
    });

    postListSuccessRate.add(ok);
    postListRequests.add(1);

    if (!ok) {
        console.error(`post list failed: status=${res.status}, board=${boardId}, sort=${sort}, page=${page}`);
    }
}

function readPostListByDate() {
    const boardId = pickBoard();
    const date    = pickDate();

    const res = http.get(
        `${BASE_URL}/api/v1/posts?boardId=${boardId}&date=${date}&size=${SIZE}`,
        {tags: {name: 'post_list_date'}},
    );

    postListDateDuration.add(res.timings.duration);

    const ok = check(res, {
        'post list by date returns 200': (r) => r.status === 200,
        'post list by date returns posts array': (r) => hasPostsArray(r),
    });

    postListDateSuccessRate.add(ok);
    postListDateRequests.add(1);

    if (!ok) {
        console.error(`post list by date failed: status=${res.status}, board=${boardId}, date=${date}`);
    }
}

function createPost(vuId) {
    const session = login(vuId);
    if (!session) {
        postCreateSuccessRate.add(false);
        postCreateRequests.add(1);
        return;
    }

    const suffix = `${vuId}-${exec.scenario.iterationInTest}-${Date.now()}`;
    const res = http.post(
        `${BASE_URL}/api/v1/posts`,
        JSON.stringify({
            boardId: WRITE_BOARD_ID,
            title: `k6-mixed-post-${suffix}`,
            content: `k6 mixed scenario post content ${suffix}`,
            category: 'GENERAL',
            tags: ['k6', 'mixed'],
            isAnonymous: false,
            isPinned: false,
            isExternalVisible: true,
            imageUrls: [],
        }),
        {
            headers: {
                'Content-Type': 'application/json',
                Cookie: session,
            },
            tags: {name: 'post_create'},
        },
    );

    postCreateDuration.add(res.timings.duration);

    const ok = check(res, {
        'post create returns 201': (r) => r.status === 201,
    });

    postCreateSuccessRate.add(ok);
    postCreateRequests.add(1);

    if (!ok) {
        console.error(`post create failed: status=${res.status}, body=${res.body}`);
    }
}

export function postMixedScenario() {
    const r = Math.random();
    if (r < WRITE_RATIO) {
        createPost(exec.vu.idInTest);
    } else if (r < WRITE_RATIO + (1 - WRITE_RATIO) * DATE_READ_RATIO) {
        readPostListByDate();
    } else {
        readPostList();
    }

    if (THINK_TIME > 0) {
        sleep(THINK_TIME);
    }
}
