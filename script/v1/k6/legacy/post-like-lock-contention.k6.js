import http from 'k6/http';
import {check, sleep} from 'k6';
import exec from 'k6/execution';
import {Counter, Rate, Trend} from 'k6/metrics';

// =============================================================================
// 목적: 조회수 증가(view_count UPDATE) 레코드 락과 좋아요(like_count UPDATE)
//       레코드 락 경합으로 인한 좋아요 응답 지연을 재현한다.
//
// 재현 원리:
//   - 특정 인기 게시글 몇 개에 다수 VU 가 동시에 상세 조회(view_count++)와
//     좋아요(like_count++)를 혼합 요청
//   - 두 작업이 같은 post 레코드 row에 UPDATE 를 경쟁하므로 레코드 락 대기 발생
//   - 좋아요 p99 지연이 조회 p99 지연보다 현저히 높으면 경합 재현 성공
//
// Example:
//   K6_WEB_DASHBOARD=true k6 run \
//     -e BASE_URL=http://localhost:8080 \
//     -e LOGIN_PASSWORD=seed12345 \
//     -e HOT_POST_IDS=5087931,5237931,5387931 \
//     -e HOT_POST_WEIGHTS=5,3,2 \
//     -e RATE=200 \
//     -e DURATION=2m \
//     -e START_RATE=20 \
//     -e WARMUP_STEPS=3 \
//     -e WARMUP_STEP_DURATION=20s \
//     -e PRE_ALLOCATED_VUS=150 \
//     -e MAX_VUS=400 \
//     -e LIKE_RATIO=0.3 \
//     script/v1/k6/post-like-lock-contention.k6.js
//
// 관전 포인트:
//   - like_duration p99 vs post_detail_duration p99 비교
//   - like_success_rate 저하 여부 (락 타임아웃 시 에러 발생)
//   - k6 web-dashboard 에서 두 Trend 를 나란히 놓고 확인
// =============================================================================

const BASE_URL             = __ENV.BASE_URL             || 'http://localhost:8080';
const LOGIN_PASSWORD       = __ENV.LOGIN_PASSWORD       || 'seed12345';
const RATE                 = Number(__ENV.RATE          || 100);
const DURATION             = __ENV.DURATION              || '2m';
const START_RATE           = Number(__ENV.START_RATE    || Math.max(1, Math.floor(RATE * 0.1)));
const WARMUP_STEPS         = Number(__ENV.WARMUP_STEPS  || 3);
const WARMUP_STEP_DURATION = __ENV.WARMUP_STEP_DURATION  || '20s';
const PRE_ALLOCATED_VUS    = Number(__ENV.PRE_ALLOCATED_VUS || 100);
const MAX_VUS              = Number(__ENV.MAX_VUS        || Math.max(PRE_ALLOCATED_VUS * 2, 200));
const THINK_TIME           = Number(__ENV.THINK_TIME    || 0);

// 좋아요 요청 비율 (나머지는 상세 조회 → 조회수 증가)
const LIKE_RATIO = Number(__ENV.LIKE_RATIO || 0.3);

// 시드 유저 범위
const SEED_USER_START = 1;
const SEED_USER_END   = 50000;

// 인기 게시글 ID 목록 (조회수 + 좋아요 둘 다 이 게시글에 집중)
const HOT_POST_IDS = (__ENV.HOT_POST_IDS || '5087931,5237931,5387931')
    .split(',')
    .map((v) => Number(v.trim()))
    .filter((v) => Number.isInteger(v) && v > 0);

const HOT_POST_WEIGHTS = (__ENV.HOT_POST_WEIGHTS || '')
    .split(',')
    .map((v) => Number(v.trim()))
    .filter((v) => Number.isFinite(v) && v > 0);

if (HOT_POST_IDS.length === 0) {
    throw new Error('HOT_POST_IDS must contain at least one post id.');
}

// 가중치 누적합 (HOT_POST_WEIGHTS 미지정 시 균등 분산)
const HOT_POST_CUMULATIVE = (() => {
    let sum = 0;
    return HOT_POST_IDS.map((postId, i) => {
        const weight = HOT_POST_WEIGHTS[i] || 1;
        sum += weight;
        return {postId, cumulativeWeight: sum};
    });
})();

const HOT_POST_TOTAL_WEIGHT = HOT_POST_CUMULATIVE[HOT_POST_CUMULATIVE.length - 1].cumulativeWeight;

function buildStages() {
    const start  = Math.max(1, Math.min(START_RATE, RATE));
    const steps  = Math.max(1, Math.floor(WARMUP_STEPS));
    const stages = [];

    for (let step = 1; step <= steps; step++) {
        const progress   = step / steps;
        const targetRate = Math.max(start, Math.round(start + (RATE - start) * progress));
        stages.push({target: targetRate, duration: WARMUP_STEP_DURATION});
    }

    stages.push({target: RATE, duration: DURATION});
    return stages;
}

export const options = {
    scenarios: {
        like_lock_contention: {
            executor: 'ramping-arrival-rate',
            exec: 'likeContestionScenario',
            startRate: Math.max(1, Math.min(START_RATE, RATE)),
            timeUnit: '1s',
            stages: buildStages(),
            preAllocatedVUs: PRE_ALLOCATED_VUS,
            maxVUs: MAX_VUS,
        },
    },
    thresholds: {
        http_req_failed:          ['rate<0.05'],
        // 좋아요가 느려지면 이 threshold 가 깨진다 — 경합 재현 확인 지점
        like_duration:            ['p(95)<500', 'p(99)<1500'],
        post_detail_duration:     ['p(95)<500', 'p(99)<1000'],
        like_success_rate:        ['rate>0.95'],
        post_detail_success_rate: ['rate>0.99'],
    },
    summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
};

const likeDuration          = new Trend('like_duration', true);
const postDetailDuration    = new Trend('post_detail_duration', true);
const likeSuccessRate       = new Rate('like_success_rate');
const postDetailSuccessRate = new Rate('post_detail_success_rate');
const likeRequests          = new Counter('like_requests');
const postDetailRequests    = new Counter('post_detail_requests');

// VU별 세션 캐시 (좋아요는 로그인 필요)
const sessionCache = {};

function pickHotPostId() {
    const random = Math.random() * HOT_POST_TOTAL_WEIGHT;
    for (const post of HOT_POST_CUMULATIVE) {
        if (random < post.cumulativeWeight) {
            return post.postId;
        }
    }
    return HOT_POST_CUMULATIVE[HOT_POST_CUMULATIVE.length - 1].postId;
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
    const res   = http.post(
        `${BASE_URL}/api/v1/auth/login`,
        JSON.stringify({email, password: LOGIN_PASSWORD}),
        {
            headers: {'Content-Type': 'application/json'},
            tags:    {name: 'auth_login'},
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

function hasPostId(response) {
    try {
        const postId = response.json('data.postId');
        return typeof postId === 'number' && postId > 0;
    } catch (_) {
        return false;
    }
}

// 상세 조회 → post row 에 view_count UPDATE 락 유발
function readPostDetail() {
    const postId  = pickHotPostId();
    const res     = http.get(
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

// 좋아요 → post row 에 like_count UPDATE 락 경합
// 좋아요/취소를 번갈아 수행해서 상태가 누적되지 않도록 함
function toggleLike(vuId) {
    const session = login(vuId);
    if (!session) {
        likeSuccessRate.add(false);
        likeRequests.add(1);
        return;
    }

    const postId  = pickHotPostId();
    const headers = {
        'Content-Type': 'application/json',
        Cookie:         session,
    };

    // 좋아요 시도
    const likeRes = http.post(
        `${BASE_URL}/api/v1/posts/${postId}/likes`,
        null,
        {headers, tags: {name: 'post_like'}},
    );

    likeDuration.add(likeRes.timings.duration);

    // 201(새로 좋아요) 또는 409(이미 좋아요) 모두 동작으로 간주
    const likeOk = check(likeRes, {
        'like returns 201 or 409': (r) => r.status === 201 || r.status === 409,
    });

    if (likeOk && likeRes.status === 201) {
        // 좋아요 성공 시 바로 취소 → 다음 반복에서 다시 경합 유발
        const unlikeRes = http.del(
            `${BASE_URL}/api/v1/posts/${postId}/likes`,
            null,
            {headers, tags: {name: 'post_unlike'}},
        );
        check(unlikeRes, {'unlike returns 200': (r) => r.status === 200});
    }

    likeSuccessRate.add(likeOk);
    likeRequests.add(1);

    if (!likeOk) {
        console.error(`like failed: status=${likeRes.status}, postId=${postId}, body=${likeRes.body}`);
    }
}

export function likeContestionScenario() {
    if (Math.random() < LIKE_RATIO) {
        toggleLike(exec.vu.idInTest);
    } else {
        readPostDetail();
    }

    if (THINK_TIME > 0) {
        sleep(THINK_TIME);
    }
}
