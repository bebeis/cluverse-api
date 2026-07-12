import http from 'k6/http';
import {check, sleep} from 'k6';
import {Counter, Rate, Trend} from 'k6/metrics';

// Example:
// k6 run \
//   -e BASE_URL=http://localhost:8080 \
//   -e RATE=100 \
//   -e DURATION=2m \
//   script/v1/k6/post-detail-read.k6.js
//
// 반영된 실제 트래픽 패턴:
//   - 인기 게시판(board_id=2001001) post 위주 80%, 일반 게시판 post 20%
//   - 인기 게시글 편중: view_count가 높은 게시글 (post_id 기준 bias)
//   - 인기 게시판 post: post_id 5000001~6000000 (1M개), view_count 5000~154999
//   - 일반 게시판 post: post_id 3000001~5000000 (2M개), view_count 0~19999
//
// Seed 데이터 기반:
//   - 인기 게시판: POPULAR_POST_START=5000001, POPULAR_POST_END=6000000
//   - 일반 게시판: NORMAL_POST_START=3000001, NORMAL_POST_END=5000000

const BASE_URL             = __ENV.BASE_URL              || 'http://localhost:8080';
const RATE                 = Number(__ENV.RATE            || 50);
const DURATION             = __ENV.DURATION               || '1m';
const PRE_ALLOCATED_VUS    = Number(__ENV.PRE_ALLOCATED_VUS || 50);
const MAX_VUS              = Number(__ENV.MAX_VUS          || Math.max(PRE_ALLOCATED_VUS * 2, 100));
const THINK_TIME           = Number(__ENV.THINK_TIME       || 0);

// 인기 게시판 post 범위 (05a seed)
const POPULAR_POST_START   = Number(__ENV.POPULAR_POST_START || 5000001);
const POPULAR_POST_END     = Number(__ENV.POPULAR_POST_END   || 6000000);
// 일반 게시판 post 범위 (05 seed)
const NORMAL_POST_START    = Number(__ENV.NORMAL_POST_START  || 3000001);
const NORMAL_POST_END      = Number(__ENV.NORMAL_POST_END    || 5000000);

// 인기 게시판 트래픽 비율 (80%)
const POPULAR_BOARD_RATIO  = Number(__ENV.POPULAR_BOARD_RATIO || 0.8);
// 인기 게시글 편중 bias (높을수록 후반부 post_id에 집중)
const POST_BIAS            = Number(__ENV.POST_BIAS || 3);

export const options = {
    scenarios: {
        post_detail_read: {
            executor: 'constant-arrival-rate',
            exec: 'readPostDetailScenario',
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
        post_detail_duration: ['p(95)<800', 'p(99)<1500'],
        post_detail_success_rate: ['rate>0.99'],
    },
    summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
};

const postDetailDuration    = new Trend('post_detail_duration', true);
const postDetailSuccessRate = new Rate('post_detail_success_rate');
const postDetailRequests    = new Counter('post_detail_requests');

// 인기 게시글 편중 post_id 선택
// view_count = 5000 + MOD((post_id - POPULAR_POST_START) * 29, 150000)
// view_count가 높을수록 인기 → post_id 후반부에 집중 (bias 반영)
function pickPopularPostId() {
    const range = POPULAR_POST_END - POPULAR_POST_START + 1;
    const bias = POST_BIAS > 0 ? POST_BIAS : 3;
    // Math.pow(random, 1/bias): bias가 클수록 후반부(view_count 높은 쪽)에 집중
    const offset = Math.floor(Math.pow(Math.random(), 1 / bias) * range);
    return POPULAR_POST_START + Math.min(offset, range - 1);
}

// 일반 게시판은 균등 분산
function pickNormalPostId() {
    const range = NORMAL_POST_END - NORMAL_POST_START + 1;
    return NORMAL_POST_START + Math.floor(Math.random() * range);
}

function pickPostId() {
    return Math.random() < POPULAR_BOARD_RATIO
        ? pickPopularPostId()
        : pickNormalPostId();
}

function hasPostId(response) {
    try {
        const postId = response.json('data.postId');
        return typeof postId === 'number' && postId > 0;
    } catch (_error) {
        return false;
    }
}

export function readPostDetailScenario() {
    const postId = pickPostId();
    const response = http.get(
        `${BASE_URL}/api/v1/posts/${postId}`,
        {tags: {name: 'post_detail_read'}},
    );

    postDetailDuration.add(response.timings.duration);

    const ok = check(response, {
        'post detail returns 200': (res) => res.status === 200,
        'post detail returns postId': (res) => hasPostId(res),
    });

    postDetailSuccessRate.add(ok);
    postDetailRequests.add(1);

    if (!ok) {
        console.error(`post detail failed: status=${response.status}, postId=${postId}, body=${response.body}`);
    }

    if (THINK_TIME > 0) {
        sleep(THINK_TIME);
    }
}
