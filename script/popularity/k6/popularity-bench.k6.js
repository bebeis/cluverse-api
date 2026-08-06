import http from 'k6/http';
import { check } from 'k6';
import { Rate, Trend } from 'k6/metrics';
import exec from 'k6/execution';

const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';
const version = (__ENV.VERSION || '').toLowerCase();
const token = __ENV.BENCHMARK_TOKEN || '';
const postIdMin = Number(__ENV.POST_ID_MIN || 5900000);
const postIdMax = Number(__ENV.POST_ID_MAX || 5999999);
const iterations = Number(__ENV.ITERATIONS || (version === 'v1' ? 3 : 100));
const vus = Number(__ENV.VUS || (version === 'v1' ? 1 : 10));

if (!['v1', 'v2'].includes(version)) throw new Error('VERSION은 v1 또는 v2여야 합니다.');

const evaluationDuration = new Trend('popularity_evaluation_duration', true);
const examinedPosts = new Trend('popularity_examined_posts');
const failures = new Rate('popularity_failures');

export const options = {
  summaryTrendStats: ['avg', 'p(95)', 'p(99)', 'max'],
  scenarios: {
    evaluate: { executor: 'shared-iterations', vus, iterations, maxDuration: __ENV.MAX_DURATION || '10m' },
  },
  thresholds: { popularity_failures: ['rate<0.01'] },
  tags: { version },
};

function request() {
  const headers = token ? { 'X-Benchmark-Token': token } : {};
  if (version === 'v1') {
    return http.post(`${baseUrl}/api/v1/popular-posts/promotion-runs`, null, { headers, tags: { version } });
  }
  const width = postIdMax - postIdMin + 1;
  const postId = postIdMin + (exec.scenario.iterationInTest % width);
  return http.post(`${baseUrl}/api/v2/popular-posts/${postId}/promotion-checks`, null, { headers, tags: { version } });
}

export default function () {
  const response = request();
  const successful = check(response, {
    'status is 200': (res) => res.status === 200,
    'version matches': (res) => {
      try { return String(res.json('data.version')).toLowerCase() === version; } catch (_) { return false; }
    },
  });
  evaluationDuration.add(response.timings.duration);
  failures.add(!successful);
  if (successful) examinedPosts.add(version === 'v1' ? Number(response.json('data.examinedPostCount')) : 1);
}

export function handleSummary(data) {
  const p99 = data.metrics.popularity_evaluation_duration?.values?.['p(99)'] ?? 0;
  const examined = data.metrics.popularity_examined_posts?.values?.avg ?? 0;
  return { stdout: `\n[${version}] p99_ms=${p99.toFixed(2)}, examined_posts_avg=${examined.toFixed(2)}\n` };
}
