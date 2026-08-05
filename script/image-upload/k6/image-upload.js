import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

const BASE_URL = (__ENV.BASE_URL || 'http://localhost:8080').replace(/\/$/, '');
const VERSION = __ENV.VERSION || 'v1';
const TOKEN = __ENV.BENCHMARK_TOKEN || '';
const IMAGE_FILE = __ENV.IMAGE_FILE;
const IMAGE_COUNT = Number(__ENV.IMAGE_COUNT || 3);
const IMAGE_MIME_TYPE = __ENV.IMAGE_MIME_TYPE || 'image/jpeg';
const IMAGE_NAME = __ENV.IMAGE_NAME || 'benchmark.jpg';
const SAFE_IMAGE_NAME = IMAGE_NAME.replace(/[^A-Za-z0-9._-]/g, '_');
const SUMMARY_PATH = __ENV.SUMMARY_PATH || `results/${VERSION}-summary.json`;

if (!IMAGE_FILE) {
  throw new Error('IMAGE_FILE is required');
}

const imageBytes = open(IMAGE_FILE, 'b');
const uploadFailures = new Rate('image_upload_failures');
const controlFailures = new Rate('control_api_failures');
const uploadRequests = new Counter('image_upload_requests');
const uploadDuration = new Trend('image_upload_duration', true);
const outputBytes = new Trend('image_upload_output_bytes');
const reductionPercent = new Trend('image_upload_reduction_percent');

export const options = {
  summaryTrendStats: ['avg', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
  scenarios: {
    uploads: {
      executor: 'constant-vus',
      exec: 'upload',
      vus: Number(__ENV.VUS || 4),
      duration: __ENV.DURATION || '30s',
      gracefulStop: '30s',
    },
    control_api: {
      executor: 'constant-arrival-rate',
      exec: 'controlApi',
      rate: Number(__ENV.CONTROL_RATE || 5),
      timeUnit: '1s',
      duration: __ENV.DURATION || '30s',
      preAllocatedVUs: 4,
      maxVUs: 20,
      gracefulStop: '5s',
    },
  },
  thresholds: {
    'image_upload_failures': ['rate<0.01'],
    'control_api_failures': ['rate<0.01'],
    'http_req_duration{endpoint:upload}': ['p(95)>=0'],
    'http_req_duration{endpoint:control}': ['p(95)>=0'],
  },
};

export function upload() {
  const multipart = buildMultipart(uuid());
  const response = http.post(
    `${BASE_URL}/api/${VERSION}/image-uploads`,
    multipart.body,
    {
      headers: {
        'X-Benchmark-Token': TOKEN,
        'Content-Type': `multipart/form-data; boundary=${multipart.boundary}`,
      },
      tags: { endpoint: 'upload', version: VERSION },
      timeout: __ENV.REQUEST_TIMEOUT || '90s',
    },
  );

  const success = check(response, {
    'upload completed': (res) => res.status === 201 && res.json('data.status') === 'COMPLETED',
  });
  uploadRequests.add(1, { version: VERSION });
  uploadFailures.add(!success, { version: VERSION });
  if (!success) sleep(0.1);
  uploadDuration.add(response.timings.duration, { version: VERSION });
  if (success) {
    outputBytes.add(Number(response.json('data.outputBytes')), { version: VERSION });
    reductionPercent.add(Number(response.json('data.reductionPercent')), { version: VERSION });
  }
}

function buildMultipart(requestId) {
  const boundary = `----cluverse-k6-${uuid()}`;
  const chunks = [];
  appendText(chunks, boundary, 'requestId', requestId);
  appendText(chunks, boundary, 'failurePoint', 'NONE');
  for (let index = 0; index < IMAGE_COUNT; index += 1) {
    chunks.push(ascii(
      `--${boundary}\r\n` +
      `Content-Disposition: form-data; name="images"; filename="${index}-${SAFE_IMAGE_NAME}"\r\n` +
      `Content-Type: ${IMAGE_MIME_TYPE}\r\n\r\n`,
    ));
    chunks.push(new Uint8Array(imageBytes));
    chunks.push(ascii('\r\n'));
  }
  chunks.push(ascii(`--${boundary}--\r\n`));
  return { boundary, body: join(chunks).buffer };
}

function appendText(chunks, boundary, name, value) {
  chunks.push(ascii(
    `--${boundary}\r\n` +
    `Content-Disposition: form-data; name="${name}"\r\n\r\n` +
    `${value}\r\n`,
  ));
}

function join(chunks) {
  const length = chunks.reduce((total, chunk) => total + chunk.byteLength, 0);
  const joined = new Uint8Array(length);
  let offset = 0;
  for (const chunk of chunks) {
    joined.set(chunk, offset);
    offset += chunk.byteLength;
  }
  return joined;
}

function ascii(value) {
  const bytes = new Uint8Array(value.length);
  for (let index = 0; index < value.length; index += 1) {
    bytes[index] = value.charCodeAt(index);
  }
  return bytes;
}

export function controlApi() {
  const response = http.get(`${BASE_URL}/actuator/health`, {
    tags: { endpoint: 'control', version: VERSION },
    timeout: '3s',
  });
  const success = check(response, { 'control API healthy': (res) => res.status === 200 });
  controlFailures.add(!success, { version: VERSION });
}

export function handleSummary(data) {
  return {
    [SUMMARY_PATH]: JSON.stringify(data, null, 2),
    stdout: summaryLine(data),
  };
}

function summaryLine(data) {
  const metric = data.metrics.image_upload_duration;
  const requests = data.metrics.image_upload_requests;
  return `\n${VERSION} upload p95=${value(metric, 'p(95)')}ms p99=${value(metric, 'p(99)')}ms rate=${value(requests, 'rate')}/s\n`;
}

function value(metric, key) {
  if (!metric || !metric.values || metric.values[key] === undefined) return 'n/a';
  return Number(metric.values[key]).toFixed(2);
}

function uuid() {
  const hex = 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx';
  return hex.replace(/[xy]/g, (character) => {
    const random = Math.floor(Math.random() * 16);
    const value = character === 'x' ? random : (random & 0x3) | 0x8;
    return value.toString(16);
  });
}
