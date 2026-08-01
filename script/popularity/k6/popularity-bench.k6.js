// 동일 정책의 두 실행 구조를 각각의 자연스러운 처리 단위로 측정한다.
// V1 RATE = 전체 집계 실행/s, V2 RATE = 변경 이벤트/s.
import http from 'k6/http';
import { check } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

const VERSION = (__ENV.VERSION || '').toLowerCase();
if (!['v1', 'v2'].includes(VERSION)) {
    throw new Error(`VERSION은 v1|v2 중 하나여야 합니다 (현재: "${__ENV.VERSION}")`);
}

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const BENCHMARK_HEADER = __ENV.BENCHMARK_HEADER || 'X-Benchmark-Token';
const BENCHMARK_TOKEN = __ENV.BENCHMARK_TOKEN || '';
const RATE = Number(__ENV.RATE || (VERSION === 'v1' ? 1 : 100));
const DURATION = __ENV.DURATION || '1m';
const PRE_ALLOCATED_VUS = Number(__ENV.PRE_ALLOCATED_VUS || 50);
const MAX_VUS = Number(__ENV.MAX_VUS || Math.max(PRE_ALLOCATED_VUS * 2, 100));
const GRACEFUL_STOP = __ENV.GRACEFUL_STOP || '10s';
const POST_MODE = (__ENV.POST_MODE || 'range').toLowerCase();
const FIXED_POST_ID = Number(__ENV.POST_ID || 9100000001);
const POST_ID_MIN = Number(__ENV.POST_ID_MIN || 5000001);
const POST_ID_MAX = Number(__ENV.POST_ID_MAX || 5999999);

if (!['fixed', 'range'].includes(POST_MODE)) {
    throw new Error(`POST_MODE는 fixed|range 중 하나여야 합니다 (현재: "${POST_MODE}")`);
}
if (!Number.isFinite(RATE) || RATE <= 0) {
    throw new Error('RATE는 0보다 큰 숫자여야 합니다.');
}
if (POST_ID_MIN > POST_ID_MAX) {
    throw new Error('POST_ID_MIN은 POST_ID_MAX보다 클 수 없습니다.');
}

const headers = { Accept: 'application/json' };
if (BENCHMARK_TOKEN) headers[BENCHMARK_HEADER] = BENCHMARK_TOKEN;

export const options = {
    scenarios: {
        popularity_bench: {
            executor: 'constant-arrival-rate',
            exec: 'benchScenario',
            rate: RATE,
            timeUnit: '1s',
            duration: DURATION,
            preAllocatedVUs: PRE_ALLOCATED_VUS,
            maxVUs: MAX_VUS,
            gracefulStop: GRACEFUL_STOP,
        },
    },
    thresholds: {
        popularity_request_success_rate: ['rate>0.99'],
        http_req_failed: ['rate<0.01'],
    },
    summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
    tags: { version: VERSION },
};

const requestDuration = new Trend('popularity_request_duration', true);
const requestSuccessRate = new Rate('popularity_request_success_rate');
const sourceEvents = new Counter('popularity_source_events');
const promotionRuns = new Counter('popularity_promotion_runs');

function pickPostId() {
    if (POST_MODE === 'fixed') return FIXED_POST_ID;
    return POST_ID_MIN + Math.floor(Math.random() * (POST_ID_MAX - POST_ID_MIN + 1));
}

function isSuccessfulApiResponse(response) {
    if (response.status < 200 || response.status >= 300) return false;
    try {
        const code = Number(response.json('code'));
        return code >= 200 && code < 300;
    } catch (_error) {
        return false;
    }
}

export function benchScenario() {
    const postId = pickPostId();
    const url = VERSION === 'v1'
        ? `${BASE_URL}/api/v1/popular-posts/promotion-runs`
        : `${BASE_URL}/api/v2/popular-posts/${postId}/promotion-checks`;
    const name = VERSION === 'v1' ? 'popularity_full_scan' : 'popularity_incremental_check';
    const response = http.post(url, null, { headers, tags: { name } });
    const ok = check(response, { 'successful ApiResponse': isSuccessfulApiResponse });

    requestDuration.add(response.timings.duration);
    requestSuccessRate.add(ok);
    if (VERSION === 'v1') promotionRuns.add(1);
    else sourceEvents.add(1);

    if (!ok) {
        console.error(
            `[${VERSION}] postId=${postId} status=${response.status} body=${String(response.body).slice(0, 300)}`,
        );
    }
}
