import http from 'k6/http';
import {check, sleep} from 'k6';
import exec from 'k6/execution';
import {Counter, Rate, Trend} from 'k6/metrics';

// Example:
// k6 run \
//   --out web-dashboard \
//   -e BASE_URL=http://localhost:8080 \
//   -e LOGIN_PASSWORD=seed12345 \
//   -e BOARD_IDS=2001001,2001002,2000001 \
//   -e POPULAR_BOARD_ID=2001001 \
//   -e WRITE_BOARD_ID=2001001 \
//   -e MAX_PAGE=500 \
//   -e RATE=200 \
//   -e DURATION=2m \
//   -e PRE_ALLOCATED_VUS=300 \
//   -e MAX_VUS=300 \
//   script/v1/k6/post-full.k6.js
//
// 반영된 실제 트래픽 패턴:
//   - 목록 조회 75% / 상세 조회 15% / 쓰기 10%
//   - 목록 조회 중: 페이지 기반 85% / 날짜 기반 15%
//   - 상세 조회: 인기 게시판(board_id=2001001) post 80%, 일반 게시판 post 20%
//   - 상세 조회 인기 게시글 편중: view_count 높은 쪽 bias (post_id 후반부 집중)
//   - 게시판 선택: 인기 게시판 가중치 7, 일반 게시판 가중치 1
//   - 정렬: LATEST 80% / VIEW_COUNT 20%
//   - 앞 페이지 편향 랜덤 페이지 (최대 MAX_PAGE)
//   - 날짜 기반 조회: 최근 2년 이내 랜덤 날짜
//   - 쓰기: VU별 세션 유지 (1회 로그인 후 재사용)
//
// Seed 데이터 기반:
//   - 인기 게시판 post: post_id 5000001~6000000 (1M개), view_count 5000~154999
//   - 일반 게시판 post: post_id 3000001~5000000 (2M개), view_count 0~19999
//   - 시드 유저: seed_user_000001@seed.local ~ seed_user_050000@seed.local

const BASE_URL          = __ENV.BASE_URL          || 'http://localhost:8080';
const LOGIN_PASSWORD    = __ENV.LOGIN_PASSWORD    || 'seed12345';
const WRITE_BOARD_ID    = Number(__ENV.WRITE_BOARD_ID   || 2001001);
const MAX_PAGE          = Number(__ENV.MAX_PAGE          || 500);
const PAGE_BIAS         = Number(__ENV.PAGE_BIAS         || 3);
const DATE_READ_RATIO   = Number(__ENV.DATE_READ_RATIO   || 0.15);
const SIZE              = 10;
const RATE              = Number(__ENV.RATE              || 50);
const DURATION          = __ENV.DURATION                 || '1m';
const PRE_ALLOCATED_VUS = Number(__ENV.PRE_ALLOCATED_VUS || 50);
const MAX_VUS           = Number(__ENV.MAX_VUS           || Math.max(PRE_ALLOCATED_VUS * 2, 100));
const THINK_TIME        = Number(__ENV.THINK_TIME        || 0);

// 트래픽 비율
// 목록 75% / 상세 15% / 쓰기 10%
const WRITE_RATIO  = 0.10;
const DETAIL_RATIO = 0.15;
// 목록 조회 내: 페이지 기반 85%, 날짜 기반 15%

// 상세 조회: 인기 게시판 80% / 일반 게시판 20%
const POPULAR_DETAIL_RATIO = Number(__ENV.POPULAR_DETAIL_RATIO || 0.8);
const DETAIL_POST_BIAS     = Number(__ENV.POST_BIAS            || 3);

// 인기 게시판 post 범위 (05a seed)
const POPULAR_POST_START = Number(__ENV.POPULAR_POST_START || 5000001);
const POPULAR_POST_END   = Number(__ENV.POPULAR_POST_END   || 6000000);
// 일반 게시판 post 범위 (05 seed)
const NORMAL_POST_START  = Number(__ENV.NORMAL_POST_START  || 3000001);
const NORMAL_POST_END    = Number(__ENV.NORMAL_POST_END    || 5000000);

// 게시판 목록: 인기 게시판 가중치 7, 일반 게시판 가중치 1
const BOARD_WEIGHTS = (() => {
    const raw       = __ENV.BOARD_IDS    || '2001001,2001002,2000001';
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

// 시드 유저 범위
const SEED_USER_START = 1;
const SEED_USER_END   = 50000;

export const options = {
    scenarios: {
        post_full: {
            executor: 'constant-arrival-rate',
            exec: 'postFullScenario',
            rate: RATE,
            timeUnit: '1s',
            duration: DURATION,
            preAllocatedVUs: PRE_ALLOCATED_VUS,
            maxVUs: MAX_VUS,
        },
    },
    thresholds: {
        http_req_failed:              ['rate<0.01'],
        http_req_duration:            ['p(95)<1500', 'p(99)<3000'],
        post_list_duration:           ['p(95)<800',  'p(99)<1500'],
        post_list_date_duration:      ['p(95)<500',  'p(99)<1000'],
        post_detail_duration:         ['p(95)<800',  'p(99)<1500'],
        post_create_duration:         ['p(95)<1000', 'p(99)<2000'],
        post_list_success_rate:       ['rate>0.99'],
        post_list_date_success_rate:  ['rate>0.99'],
        post_detail_success_rate:     ['rate>0.99'],
        post_create_success_rate:     ['rate>0.99'],
    },
    summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
};

const postListDuration         = new Trend('post_list_duration', true);
const postListDateDuration     = new Trend('post_list_date_duration', true);
const postDetailDuration       = new Trend('post_detail_duration', true);
const postCreateDuration       = new Trend('post_create_duration', true);
const postListSuccessRate      = new Rate('post_list_success_rate');
const postListDateSuccessRate  = new Rate('post_list_date_success_rate');
const postDetailSuccessRate    = new Rate('post_detail_success_rate');
const postCreateSuccessRate    = new Rate('post_create_success_rate');
const postListRequests         = new Counter('post_list_requests');
const postListDateRequests     = new Counter('post_list_date_requests');
const postDetailRequests       = new Counter('post_detail_requests');
const postCreateRequests       = new Counter('post_create_requests');
const postListSelectedPage     = new Trend('post_list_selected_page');

// VU별 세션 캐시
const sessionCache = {};

// ── 공통 유틸 ──────────────────────────────────────────────

function pickBoard() {
    const r = Math.random() * BOARD_TOTAL_WEIGHT;
    for (const b of BOARD_CUMULATIVE) {
        if (r < b.cum) return b.boardId;
    }
    return BOARD_CUMULATIVE[BOARD_CUMULATIVE.length - 1].boardId;
}

function pickPage() {
    const maxPage = Math.max(1, Math.floor(MAX_PAGE));
    const bias    = PAGE_BIAS > 0 ? PAGE_BIAS : 3;
    return Math.min(maxPage, Math.floor(Math.pow(Math.random(), bias) * maxPage) + 1);
}

function pickSort() {
    return Math.random() < SORT_LATEST_RATIO ? 'LATEST' : 'VIEW_COUNT';
}

function pickDate() {
    const now      = new Date();
    const msPerDay = 24 * 60 * 60 * 1000;
    const randomMs = Math.floor(Math.random() * 365 * 2) * msPerDay;
    return new Date(now.getTime() - randomMs).toISOString().slice(0, 10);
}

// 인기 게시글 편중: view_count 높은 쪽(post_id 후반부)에 집중
// seed: view_count = 5000 + MOD((post_id - 5000000) * 29, 150000)
// → post_id 후반부가 반드시 고조회는 아니지만, 편중 패턴 근사
function pickPopularPostId() {
    const range  = POPULAR_POST_END - POPULAR_POST_START + 1;
    const bias   = DETAIL_POST_BIAS > 0 ? DETAIL_POST_BIAS : 3;
    const offset = Math.floor(Math.pow(Math.random(), 1 / bias) * range);
    return POPULAR_POST_START + Math.min(offset, range - 1);
}

function pickNormalPostId() {
    const range = NORMAL_POST_END - NORMAL_POST_START + 1;
    return NORMAL_POST_START + Math.floor(Math.random() * range);
}

function pickPostId() {
    return Math.random() < POPULAR_DETAIL_RATIO
        ? pickPopularPostId()
        : pickNormalPostId();
}

function seedEmail(vuId) {
    const idx = ((vuId - 1) % (SEED_USER_END - SEED_USER_START + 1)) + SEED_USER_START;
    return `seed_user_${String(idx).padStart(6, '0')}@seed.local`;
}

function login(vuId) {
    if (sessionCache[vuId]) return sessionCache[vuId];

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
        'login returns 200':        (r) => r.status === 200,
        'login returns JSESSIONID': (r) => Boolean(r.cookies.JSESSIONID && r.cookies.JSESSIONID.length > 0),
    });

    if (!ok) {
        console.warn(`login failed for ${email}: status=${res.status}`);
        return null;
    }

    sessionCache[vuId] = `JSESSIONID=${res.cookies.JSESSIONID[0].value}`;
    return sessionCache[vuId];
}

function hasPostsArray(res) {
    try { return Array.isArray(res.json('data.posts')); } catch (_) { return false; }
}

function hasPostId(res) {
    try {
        const id = res.json('data.postId');
        return typeof id === 'number' && id > 0;
    } catch (_) { return false; }
}

// ── 시나리오 함수 ───────────────────────────────────────────

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
        'post list returns 200':        (r) => r.status === 200,
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
        'post list by date returns 200':        (r) => r.status === 200,
        'post list by date returns posts array': (r) => hasPostsArray(r),
    });

    postListDateSuccessRate.add(ok);
    postListDateRequests.add(1);

    if (!ok) {
        console.error(`post list by date failed: status=${res.status}, board=${boardId}, date=${date}`);
    }
}

function readPostDetail() {
    const postId = pickPostId();

    const res = http.get(
        `${BASE_URL}/api/v1/posts/${postId}`,
        {tags: {name: 'post_detail'}},
    );

    postDetailDuration.add(res.timings.duration);

    const ok = check(res, {
        'post detail returns 200':    (r) => r.status === 200,
        'post detail returns postId': (r) => hasPostId(r),
    });

    postDetailSuccessRate.add(ok);
    postDetailRequests.add(1);

    if (!ok) {
        console.error(`post detail failed: status=${res.status}, postId=${postId}`);
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
            title: `k6-full-post-${suffix}`,
            content: `k6 full scenario post content ${suffix}`,
            category: 'GENERAL',
            tags: ['k6', 'full'],
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

// ── 메인 시나리오 ────────────────────────────────────────────
//
// 확률 분기:
//   [0, 0.10)               → createPost  (10%)
//   [0.10, 0.25)            → readPostDetail (15%)
//   [0.25, 0.25 + 0.75*0.15) → readPostListByDate (≈11.25%)
//   나머지                  → readPostList (≈63.75%)

export function postFullScenario() {
    const r = Math.random();

    if (r < WRITE_RATIO) {
        createPost(exec.vu.idInTest);
    } else if (r < WRITE_RATIO + DETAIL_RATIO) {
        readPostDetail();
    } else if (r < WRITE_RATIO + DETAIL_RATIO + (1 - WRITE_RATIO - DETAIL_RATIO) * DATE_READ_RATIO) {
        readPostListByDate();
    } else {
        readPostList();
    }

    if (THINK_TIME > 0) {
        sleep(THINK_TIME);
    }
}
