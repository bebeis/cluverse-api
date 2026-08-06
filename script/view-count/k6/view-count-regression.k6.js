import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';
import exec from 'k6/execution';

const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';
const version = (__ENV.VERSION || 'v2').toLowerCase();
const rate = Number(__ENV.RATE || 200);
const duration = __ENV.DURATION || '5m';
const postId = Number(__ENV.POST_ID || 5999999);
const readerIntervalMs = Number(__ENV.READER_INTERVAL_MS || 0);

if (!['v2', 'v3', 'v4'].includes(version)) {
  throw new Error('VERSION은 v2, v3, v4 중 하나여야 합니다.');
}

const requestFailures = new Rate('view_count_failures');
const regressions = new Counter('view_count_regressions');
const regressionSamples = new Counter('view_count_regression_samples');
const regressionDepth = new Trend('view_count_regression_depth');
const regressionRecovery = new Trend('view_count_regression_recovery_ms', true);

export const options = {
  summaryTrendStats: ['avg', 'p(95)', 'p(99)', 'max'],
  scenarios: {
    writer: {
      executor: 'constant-arrival-rate',
      exec: 'writer',
      rate,
      timeUnit: '1s',
      duration,
      preAllocatedVUs: Number(__ENV.PRE_ALLOCATED_VUS || Math.max(20, rate)),
      maxVUs: Number(__ENV.MAX_VUS || Math.max(100, rate * 4)),
    },
    reader: {
      executor: 'constant-vus',
      exec: 'reader',
      vus: Number(__ENV.READER_VUS || 1),
      duration,
    },
  },
  thresholds: { view_count_failures: ['rate<0.01'] },
  tags: { version, post_mode: 'regression' },
};

function count(cookieId) {
  return http.post(`${baseUrl}/api/${version}/posts/${postId}/view-count`, null, {
    headers: { Cookie: `cluverse_viewer=${cookieId}` },
    tags: { endpoint: 'view-count', version },
  });
}

export function writer() {
  const uniqueCookie = `k6-regression-${version}-${exec.vu.idInTest}-${exec.scenario.iterationInTest}`;
  const response = count(uniqueCookie);
  const successful = check(response, {
    'writer status is 200': (res) => res.status === 200,
  });
  requestFailures.add(!successful);
}

// 모듈 상태는 VU별로 격리되므로 reader가 여러 VU여도 각자 독립 추적한다.
// 단, 같은 flush 이벤트를 여러 VU가 중복 집계할 수 있어 이벤트 수는 존재 증명으로만 쓴다.
const readerCookie = `k6-regression-reader-${Date.now()}-${__VU}`;
let maxSeen = -Infinity;
let inRegression = false;
let regressionStartMs = 0;
let deepestDrop = 0;

export function reader() {
  const response = count(readerCookie);
  let current = NaN;
  try {
    current = Number(response.json('data.viewCount'));
  } catch (_) { /* 실패 응답은 아래 check에서 집계 */ }
  const successful = check(response, {
    'reader gets a visible count': () => Number.isFinite(current),
  });
  requestFailures.add(!successful);
  if (!successful) return;

  if (current < maxSeen) {
    regressionSamples.add(1);
    const drop = maxSeen - current;
    if (!inRegression) {
      inRegression = true;
      regressionStartMs = Date.now();
      deepestDrop = drop;
      regressions.add(1);
    } else if (drop > deepestDrop) {
      deepestDrop = drop;
    }
  } else {
    if (inRegression) {
      regressionDepth.add(deepestDrop);
      regressionRecovery.add(Date.now() - regressionStartMs);
      inRegression = false;
    }
    maxSeen = current;
  }
  if (readerIntervalMs > 0) sleep(readerIntervalMs / 1000);
}

export function handleSummary(data) {
  const events = data.metrics.view_count_regressions?.values?.count ?? 0;
  const samples = data.metrics.view_count_regression_samples?.values?.count ?? 0;
  const maxDepth = data.metrics.view_count_regression_depth?.values?.max ?? 0;
  const maxRecovery = data.metrics.view_count_regression_recovery_ms?.values?.max ?? 0;
  const lines = [
    `[${version}/regression] 역행 이벤트=${events}, 역행 관측 샘플=${samples}`,
    `최대 하락폭=${maxDepth}, 최대 회복 시간=${maxRecovery.toFixed(0)}ms`,
    version === 'v4'
      ? '기대: 정상 체크포인트 경로에서는 역행 이벤트 0'
      : '기대: flush handoff에서 역행 이벤트 1회 이상 관측',
  ];
  return { stdout: `\n${lines.join('\n')}\n` };
}
