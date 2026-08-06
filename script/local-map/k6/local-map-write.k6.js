import http from 'k6/http';
import exec from 'k6/execution';
import { check } from 'k6';
import { Rate, Trend } from 'k6/metrics';

const VERSION = (__ENV.VERSION || '').toLowerCase();
if (!['v1', 'v2'].includes(VERSION)) throw new Error('VERSION은 v1 또는 v2여야 합니다.');

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const AUTH_TOKEN = __ENV.AUTH_TOKEN || '';
const BENCHMARK_TOKEN = __ENV.BENCHMARK_TOKEN || '';
const BOARD_ID = Number(__ENV.BOARD_ID || 1);
const QUERY = __ENV.QUERY || '연세대 카페';
const RATE = Number(__ENV.RATE || 20);
const DURATION = __ENV.DURATION || '30s';
const STUB_DELAY_MS = Number(__ENV.STUB_DELAY_MS || 300);

if (!AUTH_TOKEN) throw new Error('AUTH_TOKEN이 필요합니다.');
if (!BENCHMARK_TOKEN) throw new Error('BENCHMARK_TOKEN이 필요합니다.');

export const options = {
  scenarios: {
    write: {
      executor: 'constant-arrival-rate',
      exec: 'write',
      rate: RATE,
      timeUnit: '1s',
      duration: DURATION,
      preAllocatedVUs: Number(__ENV.PRE_ALLOCATED_VUS || 600),
      maxVUs: Number(__ENV.MAX_VUS || 3000),
    },
  },
  thresholds: {
    local_map_write_success: ['rate>=0.99'],
    local_map_write_duration: ['p(99)<1000'],
    dropped_iterations: ['count==0'],
  },
  summaryTrendStats: ['avg', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
  tags: { version: VERSION, offered_rps: String(RATE), stub_delay_ms: String(STUB_DELAY_MS) },
};

const writeDuration = new Trend('local_map_write_duration', true);
const writeSuccess = new Rate('local_map_write_success');
const authHeaders = {
  Accept: 'application/json',
  'Content-Type': 'application/json',
  Authorization: `Bearer ${AUTH_TOKEN}`,
};

function apiOk(response) {
  return response.status >= 200 && response.status < 300
    && Number(response.json('code')) >= 200 && Number(response.json('code')) < 300;
}

export function setup() {
  const readiness = http.get(`${BASE_URL}/api/v1/local-map/benchmark-readiness`, {
    headers: { 'X-Benchmark-Token': BENCHMARK_TOKEN },
  });
  const safe = readiness.status === 200
    && readiness.json('data.providerMode') === 'STUB'
    && readiness.json('data.experimentEndpointsEnabled') === true
    && readiness.json('data.stubProvider') === true;
  if (!safe) exec.test.abort(`실제 provider 호출 위험: readiness=${readiness.status} ${readiness.body}`);

  const searchHeaders = { ...authHeaders };
  if (VERSION === 'v1') searchHeaders['X-Benchmark-Token'] = BENCHMARK_TOKEN;
  const search = http.get(`${BASE_URL}/api/${VERSION}/places/search?query=${encodeURIComponent(QUERY)}`, {
    headers: searchHeaders,
  });
  if (!apiOk(search)) exec.test.abort(`검색 준비 실패: ${search.status} ${search.body}`);
  const first = search.json('data.places.0');
  return VERSION === 'v1'
    ? { query: first.query, sourceFingerprint: first.sourceFingerprint }
    : { selectionToken: first.selectionToken };
}

function uuid() {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (value) => {
    const random = Math.floor(Math.random() * 16);
    return (value === 'x' ? random : ((random & 3) | 8)).toString(16);
  });
}

export function write(selection) {
  const post = {
    boardId: BOARD_ID,
    title: `로컬맵 ${VERSION} ${uuid()}`,
    content: '로컬맵 트랜잭션 경계 측정',
    category: 'INFORMATION',
    tags: [],
    isAnonymous: false,
    isPinned: false,
    isExternalVisible: true,
    imageUrls: [],
  };
  const body = VERSION === 'v1'
    ? { post, places: [{ ...selection, recommended: true }] }
    : { requestId: uuid(), post, places: [{ ...selection, recommended: true }] };
  const headers = { ...authHeaders };
  if (VERSION === 'v1') headers['X-Benchmark-Token'] = BENCHMARK_TOKEN;
  const response = http.post(`${BASE_URL}/api/${VERSION}/posts/with-places`, JSON.stringify(body), {
    headers,
    tags: { name: `local_map_write_${VERSION}` },
  });
  const ok = check(response, { 'write ApiResponse success': apiOk });
  writeDuration.add(response.timings.duration);
  writeSuccess.add(ok);
  if (!ok) console.error(`${VERSION} status=${response.status} body=${String(response.body).slice(0, 300)}`);
}
