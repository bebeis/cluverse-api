import http from 'k6/http';
import {check, sleep} from 'k6';
import {Counter, Rate, Trend} from 'k6/metrics';

// Example:
// k6 run \
//   --out web-dashboard \
//   -e BASE_URL=http://localhost:8080 \
//   -e HOT_POST_IDS=5999998,5999999,6000000 \
//   -e RATE=200 \
//   -e DURATION=2m \
//   -e START_RATE=20 \
//   -e WARMUP_STEPS=3 \
//   -e WARMUP_STEP_DURATION=20s \
//   -e PRE_ALLOCATED_VUS=100 \
//   -e MAX_VUS=300 \
//   script/v1/k6/post-hot-detail-target.k6.js
//
// 반영된 부하 패턴:
//   - 게시글 상세 조회만 수행
//   - HOT_POST_IDS 에 지정한 인기 게시글 몇 개만 반복 타격
//   - 기본은 균등 분산, HOT_POST_WEIGHTS 로 특정 게시글 가중치 가능

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const RATE = Number(__ENV.RATE || 50);
const DURATION = __ENV.DURATION || '1m';
const START_RATE = Number(__ENV.START_RATE || Math.max(1, Math.floor(RATE * 0.1)));
const WARMUP_STEPS = Number(__ENV.WARMUP_STEPS || 3);
const WARMUP_STEP_DURATION = __ENV.WARMUP_STEP_DURATION || '20s';
const PRE_ALLOCATED_VUS = Number(__ENV.PRE_ALLOCATED_VUS || 50);
const MAX_VUS = Number(__ENV.MAX_VUS || Math.max(PRE_ALLOCATED_VUS * 2, 100));
const THINK_TIME = Number(__ENV.THINK_TIME || 0);

const HOT_POST_IDS = (__ENV.HOT_POST_IDS || '5999998,5999999,6000000')
    .split(',')
    .map((value) => Number(value.trim()))
    .filter((value) => Number.isInteger(value) && value > 0);

const HOT_POST_WEIGHTS = (__ENV.HOT_POST_WEIGHTS || '')
    .split(',')
    .map((value) => Number(value.trim()))
    .filter((value) => Number.isFinite(value) && value > 0);

if (HOT_POST_IDS.length === 0) {
    throw new Error('HOT_POST_IDS must contain at least one post id.');
}

const HOT_POST_CUMULATIVE = (() => {
    let sum = 0;
    return HOT_POST_IDS.map((postId, index) => {
        const weight = HOT_POST_WEIGHTS[index] || 1;
        sum += weight;
        return {postId, cumulativeWeight: sum};
    });
})();

const HOT_POST_TOTAL_WEIGHT = HOT_POST_CUMULATIVE[HOT_POST_CUMULATIVE.length - 1].cumulativeWeight;

function buildStages() {
    const normalizedStartRate = Math.max(1, Math.min(START_RATE, RATE));
    const normalizedWarmupSteps = Math.max(1, Math.floor(WARMUP_STEPS));
    const stages = [];

    for (let step = 1; step <= normalizedWarmupSteps; step++) {
        const progress = step / normalizedWarmupSteps;
        const targetRate = Math.max(
            normalizedStartRate,
            Math.round(normalizedStartRate + (RATE - normalizedStartRate) * progress),
        );
        stages.push({target: targetRate, duration: WARMUP_STEP_DURATION});
    }

    stages.push({target: RATE, duration: DURATION});
    return stages;
}

export const options = {
    scenarios: {
        post_hot_detail_target: {
            executor: 'ramping-arrival-rate',
            exec: 'readHotPostDetailScenario',
            startRate: Math.max(1, Math.min(START_RATE, RATE)),
            timeUnit: '1s',
            stages: buildStages(),
            preAllocatedVUs: PRE_ALLOCATED_VUS,
            maxVUs: MAX_VUS,
        },
    },
    thresholds: {
        http_req_failed: ['rate<0.01'],
        http_req_duration: ['p(95)<1500', 'p(99)<3000'],
        hot_post_detail_duration: ['p(95)<800', 'p(99)<1500'],
        hot_post_detail_success_rate: ['rate>0.99'],
    },
    summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
};

const hotPostDetailDuration = new Trend('hot_post_detail_duration', true);
const hotPostDetailSuccessRate = new Rate('hot_post_detail_success_rate');
const hotPostDetailRequests = new Counter('hot_post_detail_requests');

function pickHotPostId() {
    const random = Math.random() * HOT_POST_TOTAL_WEIGHT;
    for (const post of HOT_POST_CUMULATIVE) {
        if (random < post.cumulativeWeight) {
            return post.postId;
        }
    }
    return HOT_POST_CUMULATIVE[HOT_POST_CUMULATIVE.length - 1].postId;
}

function hasPostId(response) {
    try {
        const postId = response.json('data.postId');
        return typeof postId === 'number' && postId > 0;
    } catch (_error) {
        return false;
    }
}

export function readHotPostDetailScenario() {
    const postId = pickHotPostId();
    const response = http.get(
        `${BASE_URL}/api/v1/posts/${postId}`,
        {tags: {name: 'post_hot_detail_target'}},
    );

    hotPostDetailDuration.add(response.timings.duration);

    const ok = check(response, {
        'hot post detail returns 200': (res) => res.status === 200,
        'hot post detail returns postId': (res) => hasPostId(res),
    });

    hotPostDetailSuccessRate.add(ok);
    hotPostDetailRequests.add(1);

    if (!ok) {
        console.error(`hot post detail failed: status=${response.status}, postId=${postId}`);
    }

    if (THINK_TIME > 0) {
        sleep(THINK_TIME);
    }
}
