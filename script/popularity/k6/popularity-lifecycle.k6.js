// 외부에서 조건을 변화시키는 동안 V2 판정 지연과 최근 목록 노출 시점을 함께 관찰한다.
// 이 스크립트 자체는 좋아요·댓글·조회수 값을 변경하지 않는다.
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Gauge, Rate, Trend } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const BENCHMARK_HEADER = __ENV.BENCHMARK_HEADER || 'X-Benchmark-Token';
const BENCHMARK_TOKEN = __ENV.BENCHMARK_TOKEN || '';
const POST_ID = Number(__ENV.POST_ID || 9100000003);
const EVENT_RATE = Number(__ENV.EVENT_RATE || 20);
const DURATION = __ENV.DURATION || '10m';
const POLL_INTERVAL = __ENV.POLL_INTERVAL || '1s';
const PRE_ALLOCATED_VUS = Number(__ENV.PRE_ALLOCATED_VUS || 20);
const MAX_VUS = Number(__ENV.MAX_VUS || Math.max(PRE_ALLOCATED_VUS * 2, 50));

const headers = { Accept: 'application/json' };
if (BENCHMARK_TOKEN) headers[BENCHMARK_HEADER] = BENCHMARK_TOKEN;

export const options = {
    scenarios: {
        promotion_events: {
            executor: 'constant-arrival-rate',
            exec: 'promotionEvent',
            rate: EVENT_RATE,
            timeUnit: '1s',
            duration: DURATION,
            preAllocatedVUs: PRE_ALLOCATED_VUS,
            maxVUs: MAX_VUS,
            gracefulStop: '10s',
        },
        recent_observer: {
            executor: 'constant-vus',
            exec: 'recentObserver',
            vus: 1,
            duration: DURATION,
            gracefulStop: '5s',
        },
    },
    thresholds: {
        popularity_lifecycle_check_success_rate: ['rate>0.99'],
        http_req_failed: ['rate<0.01'],
    },
    summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
    tags: { version: 'v2' },
};

const checkDuration = new Trend('popularity_lifecycle_check_duration', true);
const recentDuration = new Trend('popularity_lifecycle_recent_duration', true);
const checkSuccessRate = new Rate('popularity_lifecycle_check_success_rate');
const visibleInRecent = new Gauge('popularity_visible_in_recent');
const processStartedAt = Date.now();
let firstVisibilityRecorded = false;
const firstVisibilityDelay = new Trend('popularity_first_visibility_delay', true);

function apiSucceeded(response) {
    if (response.status < 200 || response.status >= 300) return false;
    try {
        const code = Number(response.json('code'));
        return code >= 200 && code < 300;
    } catch (_error) {
        return false;
    }
}

function containsPostId(value, targetId) {
    if (Array.isArray(value)) return value.some((item) => containsPostId(item, targetId));
    if (value === null || typeof value !== 'object') return false;
    if (Number(value.postId) === targetId) return true;
    return Object.values(value).some((item) => containsPostId(item, targetId));
}

export function promotionEvent() {
    const response = http.post(
        `${BASE_URL}/api/v2/popular-posts/${POST_ID}/promotion-checks`,
        null,
        { headers, tags: { name: 'popularity_lifecycle_check' } },
    );
    const ok = check(response, { 'promotion check succeeded': apiSucceeded });
    checkDuration.add(response.timings.duration);
    checkSuccessRate.add(ok);
}

export function recentObserver() {
    const response = http.get(`${BASE_URL}/api/v2/popular-posts/recent`, {
        headers,
        tags: { name: 'popularity_recent_observer' },
    });
    const ok = check(response, { 'recent query succeeded': apiSucceeded });
    recentDuration.add(response.timings.duration);

    let visible = false;
    if (ok) {
        try {
            visible = containsPostId(response.json('data'), POST_ID);
        } catch (_error) {
            visible = false;
        }
    }
    visibleInRecent.add(visible ? 1 : 0);
    if (visible && !firstVisibilityRecorded) {
        firstVisibilityDelay.add(Date.now() - processStartedAt);
        firstVisibilityRecorded = true;
    }
    sleepDuration(POLL_INTERVAL);
}

function sleepDuration(text) {
    const match = String(text).match(/^(\d+(?:\.\d+)?)(ms|s)$/);
    if (!match) throw new Error(`POLL_INTERVAL은 ms 또는 s 단위여야 합니다: ${text}`);
    const seconds = Number(match[1]) * (match[2] === 'ms' ? 0.001 : 1);
    sleep(seconds);
}
