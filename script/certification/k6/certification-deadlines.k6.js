import http from 'k6/http';
import exec from 'k6/execution';
import { check } from 'k6';
import { Rate, Trend } from 'k6/metrics';

const PROFILE = (__ENV.PROFILE || 'warm').toLowerCase();
if (!['warm', 'cold-burst'].includes(PROFILE)) {
  throw new Error('PROFILE은 warm 또는 cold-burst여야 합니다.');
}

const BASE_URL = (__ENV.BASE_URL || 'http://localhost:8080').replace(/\/+$/, '');
const MEMBER_ID = __ENV.MEMBER_ID || '';
const BENCHMARK_TOKEN = __ENV.BENCHMARK_TOKEN || '';
const RATE = Number(__ENV.RATE || 20);
const DURATION = __ENV.DURATION || '30s';
const BURST_VUS = Number(__ENV.BURST_VUS || 50);

if (!/^[1-9]\d*$/.test(MEMBER_ID)) throw new Error('MEMBER_ID는 양의 정수여야 합니다.');
if (!BENCHMARK_TOKEN) throw new Error('BENCHMARK_TOKEN이 필요합니다.');

export const options = {
  scenarios: PROFILE === 'warm'
    ? {
        cached: {
          executor: 'constant-arrival-rate',
          rate: RATE,
          timeUnit: '1s',
          duration: DURATION,
          preAllocatedVUs: Number(__ENV.PRE_ALLOCATED_VUS || 10),
          maxVUs: Number(__ENV.MAX_VUS || 50),
        },
      }
    : {
        coldBurst: {
          executor: 'per-vu-iterations',
          vus: BURST_VUS,
          iterations: 1,
          maxDuration: '30s',
        },
      },
  thresholds: {
    dropped_iterations: ['count==0'],
    certification_deadline_success: ['rate>0.99'],
    certification_deadline_duration: [PROFILE === 'warm' ? 'p(95)<500' : 'p(95)<3000'],
  },
  summaryTrendStats: ['avg', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
  tags: { profile: PROFILE },
};

const duration = new Trend('certification_deadline_duration', true);
const success = new Rate('certification_deadline_success');
const authHeaders = {
  Accept: 'application/json',
  Authorization: `Bearer ${MEMBER_ID}`,
};

function apiOk(response) {
  if (response.status !== 200) return false;
  try {
    const body = response.json();
    return Number(body.code) === 200 && Array.isArray(body.data) && body.data.length <= 10;
  } catch (error) {
    return false;
  }
}

export function setup() {
  const readiness = http.get(`${BASE_URL}/api/v1/certification/benchmark-readiness`, {
    headers: { 'X-Benchmark-Token': BENCHMARK_TOKEN },
  });
  const safe = readiness.status === 200
    && readiness.json('data.providerMode') === 'STUB'
    && readiness.json('data.experimentEndpointsEnabled') === true
    && readiness.json('data.stubProvider') === true;
  if (!safe) exec.test.abort(`실제 provider 호출 위험: readiness=${readiness.status} ${readiness.body}`);

  const evicted = http.del(`${BASE_URL}/api/v1/certification/benchmark-cache`, null, {
    headers: { 'X-Benchmark-Token': BENCHMARK_TOKEN },
  });
  if (evicted.status !== 200) exec.test.abort(`캐시 초기화 실패: ${evicted.status} ${evicted.body}`);

  if (PROFILE === 'warm') {
    const warmed = http.get(`${BASE_URL}/api/v1/home/certification-deadlines`, { headers: authHeaders });
    if (!apiOk(warmed)) exec.test.abort(`캐시 예열 실패: ${warmed.status} ${warmed.body}`);
  }
}

export default function () {
  const response = http.get(`${BASE_URL}/api/v1/home/certification-deadlines`, {
    headers: authHeaders,
    tags: { name: 'certification_deadlines' },
  });
  const ok = check(response, {
    'certification deadlines success': apiOk,
  });
  duration.add(response.timings.duration);
  success.add(ok);
}
