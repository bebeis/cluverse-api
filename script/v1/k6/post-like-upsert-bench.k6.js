import http from 'k6/http';
import {check, sleep} from 'k6';
import exec from 'k6/execution';
import {Counter, Rate, Trend} from 'k6/metrics';

// =============================================================================
// 목적: 좋아요 수를 별도 테이블(post_like_count)로 분리하고 UPSERT 쿼리로
//       처리한 이후, 기존 레코드 락 경합이 해소됐음을 수치로 검증한다.
//
// 비교 방법:
//   1. post-like-lock-contention.k6.js 결과 저장 (--out json=before.json)
//   2. upsert 분리 배포 후 이 스크립트를 동일 파라미터로 실행
//      --out json=after.json
//   3. like_duration p99, post_detail_duration p99 를 before/after 비교
//
// Example:
//   [Before — 락 경합 재현]
//   K6_WEB_DASHBOARD=true k6 run \
//     --out json=result/before-like-lock.json \
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
//   [After — upsert 분리 검증]
//   K6_WEB_DASHBOARD=true k6 run \
//     --out json=result/after-like-upsert.json \
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
//     script/v1/k6/post-like-upsert-bench.k6.js
//
// 성공 기준 (threshold 통과 시 개선 확인):
//   - like_duration      p(99)<300  (기존 대비 5배 이상 개선 목표)
//   - post_detail_duration p(99)<500 (기존 수준 유지)
//   - like_success_rate  >0.999
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

const LIKE_RATIO = Number(__ENV.LIKE_RATIO || 0.3);

const SEED_USER_START = 1;
const SEED_USER_END   = 50000;

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
        like_upsert_bench: {
            executor: 'ramping-arrival-rate',
            exec: 'likeUpsertBenchScenario',
            startRate: Math.max(1, Math.min(START_RATE, RATE)),
            timeUnit: '1s',
            stages: buildStages(),
            preAllocatedVUs: PRE_ALLOCATED_VUS,
            maxVUs: MAX_VUS,
        },
    },
    thresholds: {
        http_req_failed:          ['rate<0.01'],
        // upsert 분리 후 달성 목표: 기존 경합 대비 p99 대폭 감소
        like_duration:            ['p(95)<200', 'p(99)<300'],
        post_detail_duration:     ['p(95)<500', 'p(99)<800'],
        like_success_rate:        ['rate>0.999'],
        post_detail_success_rate: ['rate>0.999'],
    },
    summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
};

const likeDuration          = new Trend('like_duration', true);
const postDetailDuration    = new Trend('post_detail_duration', true);
const likeSuccessRate       = new Rate('like_success_rate');
const postDetailSuccessRate = new Rate('post_detail_success_rate');
const likeRequests          = new Counter('like_requests');
const postDetailRequests    = new Counter('post_detail_requests');

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

// 상세 조회 — upsert 분리 후에도 view_count 는 여전히 post row UPDATE
// (view_count 분리 여부와 무관하게 동일 패턴 유지)
function readPostDetail() {
    const postId = pickHotPostId();
    const res    = http.get(
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

// 좋아요 — upsert 분리 후 별도 테이블 행에만 락이 걸려 post row 와 경합 없음
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

    const likeRes = http.post(
        `${BASE_URL}/api/v1/posts/${postId}/likes`,
        null,
        {headers, tags: {name: 'post_like'}},
    );

    likeDuration.add(likeRes.timings.duration);

    const likeOk = check(likeRes, {
        'like returns 201 or 409': (r) => r.status === 201 || r.status === 409,
    });

    if (likeOk && likeRes.status === 201) {
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

export function likeUpsertBenchScenario() {
    if (Math.random() < LIKE_RATIO) {
        toggleLike(exec.vu.idInTest);
    } else {
        readPostDetail();
    }

    if (THINK_TIME > 0) {
        sleep(THINK_TIME);
    }
}
