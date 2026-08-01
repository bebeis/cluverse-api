import http from 'k6/http';
import exec from 'k6/execution';
import { check } from 'k6';
import { Counter } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const AUTH_TOKEN = __ENV.AUTH_TOKEN || '';
const BENCHMARK_TOKEN = __ENV.BENCHMARK_TOKEN || '';
const BOARD_ID = Number(__ENV.BOARD_ID || 1);
const headers = { 'Content-Type': 'application/json', Authorization: `Bearer ${AUTH_TOKEN}` };
const tamperedAccepted = new Counter('tampered_token_accepted');
const idempotentWriteFailures = new Counter('idempotent_write_failures');

export const options = {
  scenarios: {
    tamper: { executor: 'shared-iterations', exec: 'tamper', vus: 1, iterations: 1, maxDuration: '10s' },
    idempotency: {
      executor: 'shared-iterations', exec: 'idempotency', vus: 10, iterations: 10,
      startTime: '1s', maxDuration: '30s',
    },
  },
  thresholds: {
    tampered_token_accepted: ['count==0'],
    idempotent_write_failures: ['count==0'],
    checks: ['rate==1'],
  },
};

export function setup() {
  if (!AUTH_TOKEN || !BENCHMARK_TOKEN) exec.test.abort('AUTH_TOKEN과 BENCHMARK_TOKEN이 필요합니다.');
  const readiness = http.get(`${BASE_URL}/api/v1/local-map/benchmark-readiness`, {
    headers: { 'X-Benchmark-Token': BENCHMARK_TOKEN },
  });
  if (readiness.json('data.providerMode') !== 'STUB' || readiness.json('data.stubProvider') !== true) {
    exec.test.abort('정합성 테스트는 STUB provider에서만 실행할 수 있습니다.');
  }
  const search = http.get(`${BASE_URL}/api/v2/places/search?query=${encodeURIComponent('연세대 카페')}`, { headers });
  const token = search.json('data.places.0.selectionToken');
  return {
    token,
    tamperRequestId: '11111111-2222-4333-8444-555555555555',
    idempotencyRequestId: 'aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee',
  };
}

function body(requestId, token) {
  return JSON.stringify({
    requestId,
    post: {
      boardId: BOARD_ID, title: '동시성 검증', content: '같은 requestId', category: 'INFORMATION',
      tags: [], isAnonymous: false, isPinned: false, isExternalVisible: true, imageUrls: [],
    },
    places: [{ selectionToken: token, recommended: true }],
  });
}

export function tamper(data) {
  const token = `${data.token.slice(0, -1)}${data.token.endsWith('a') ? 'b' : 'a'}`;
  const response = http.post(
    `${BASE_URL}/api/v2/posts/with-places`, body(data.tamperRequestId, token), { headers },
  );
  const rejected = check(response, { 'tampered token rejected': (value) => value.status === 400 });
  if (!rejected) tamperedAccepted.add(1);
}

export function idempotency(data) {
  const response = http.post(
    `${BASE_URL}/api/v2/posts/with-places`, body(data.idempotencyRequestId, data.token), { headers },
  );
  const accepted = check(response, {
    'idempotent retry accepted': (value) => value.status === 201 && Number(value.json('data.postId')) > 0,
  });
  if (!accepted) idempotentWriteFailures.add(1);
}
