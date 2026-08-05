import http from 'k6/http';
import { check } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';
import exec from 'k6/execution';

const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';
const version = (__ENV.VERSION || '').toLowerCase();
const rate = Number(__ENV.RATE || 100);
const duration = __ENV.DURATION || '1m';
const postMode = (__ENV.POST_MODE || 'hot').toLowerCase();
const postId = Number(__ENV.POST_ID || 5999999);
const postIdMin = Number(__ENV.POST_ID_MIN || postId);
const postIdMax = Number(__ENV.POST_ID_MAX || postId);

if (!['v1', 'v2', 'v3', 'v4'].includes(version)) {
  throw new Error('VERSION은 v1, v2, v3, v4 중 하나여야 합니다.');
}
if (!['hot', 'distributed'].includes(postMode)) {
  throw new Error('POST_MODE는 hot 또는 distributed여야 합니다.');
}

const requestDuration = new Trend('view_count_duration', true);
const requestFailures = new Rate('view_count_failures');
const countedViews = new Counter('view_count_counted');

export const options = {
  summaryTrendStats: ['avg', 'p(95)', 'p(99)', 'max'],
  scenarios: {
    view_count: {
      executor: 'constant-arrival-rate',
      rate,
      timeUnit: '1s',
      duration,
      preAllocatedVUs: Number(__ENV.PRE_ALLOCATED_VUS || Math.max(20, rate)),
      maxVUs: Number(__ENV.MAX_VUS || Math.max(100, rate * 4)),
    },
  },
  thresholds: { view_count_failures: ['rate<0.01'] },
  tags: { version, post_mode: postMode },
};

function targetPostId() {
  if (postMode === 'hot' || postIdMin === postIdMax) return postId;
  const width = postIdMax - postIdMin + 1;
  return postIdMin + (exec.scenario.iterationInTest % width);
}

export default function () {
  const target = targetPostId();
  const uniqueCookie = `k6-${version}-${exec.vu.idInTest}-${exec.scenario.iterationInTest}`;
  const response = http.post(`${baseUrl}/api/${version}/posts/${target}/view-count`, null, {
    headers: { Cookie: `cluverse_viewer=${uniqueCookie}` },
    tags: { endpoint: 'view-count', version, post_mode: postMode },
  });
  const successful = check(response, {
    'status is 200': (res) => res.status === 200,
    'response contains current count': (res) => {
      try { return Number.isFinite(res.json('data.viewCount')); } catch (_) { return false; }
    },
  });
  requestDuration.add(response.timings.duration);
  requestFailures.add(!successful);
  if (successful && response.json('data.counted') === true) countedViews.add(1);
}

export function handleSummary(data) {
  const p99 = data.metrics.view_count_duration?.values?.['p(99)'] ?? 0;
  const requests = data.metrics.http_reqs?.values?.count ?? 0;
  const seconds = data.state?.testRunDurationMs ? data.state.testRunDurationMs / 1000 : 0;
  const achievedRps = seconds > 0 ? requests / seconds : 0;
  return { stdout: `\n[${version}/${postMode}] requests=${requests}, achieved_rps=${achievedRps.toFixed(2)}, p99_ms=${p99.toFixed(2)}\n` };
}
