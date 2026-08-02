import http from 'k6/http';
import exec from 'k6/execution';
import { check } from 'k6';
import { Rate, Trend } from 'k6/metrics';

const VERSION = (__ENV.VERSION || '').toLowerCase();
if (!['v1', 'v2'].includes(VERSION)) throw new Error('VERSION은 v1 또는 v2여야 합니다.');

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const POST_ID = Number(__ENV.POST_ID || 0);
const AUTH_TOKEN = __ENV.AUTH_TOKEN || '';
const LIMIT = Number(__ENV.LIMIT || 100);
const CURSOR_STEPS = Number(__ENV.CURSOR_STEPS || 0);
const RATE = Number(__ENV.RATE || 20);
const DURATION = __ENV.DURATION || '30s';
const COMMENT_COUNT = __ENV.COMMENT_COUNT || 'unknown';
const TREE_SHAPE = __ENV.TREE_SHAPE || 'mixed';

if (!POST_ID) throw new Error('POST_ID가 필요합니다.');

export const options = {
  scenarios: {
    read: {
      executor: 'constant-arrival-rate',
      exec: 'readPage',
      rate: RATE,
      timeUnit: '1s',
      duration: DURATION,
      preAllocatedVUs: Number(__ENV.PRE_ALLOCATED_VUS || 20),
      maxVUs: Number(__ENV.MAX_VUS || 100),
    },
  },
  thresholds: {
    comment_request_success: ['rate>0.99'],
    comment_api_duration: ['p(95)<3000'],
  },
  summaryTrendStats: ['avg', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
  tags: {
    version: VERSION,
    comments: COMMENT_COUNT,
    tree_shape: TREE_SHAPE,
    cursor_position: CURSOR_STEPS === 0 ? 'first' : `page-${CURSOR_STEPS}`,
  },
};

const commentDuration = new Trend('comment_api_duration', true);
const detailScreenDuration = new Trend('detail_screen_duration', true);
const requestSuccess = new Rate('comment_request_success');

function headers() {
  const values = { Accept: 'application/json' };
  if (AUTH_TOKEN) values.Authorization = `Bearer ${AUTH_TOKEN}`;
  return values;
}

function apiOk(response) {
  if (response.status !== 200) return false;
  try {
    return Number(response.json('code')) === 200;
  } catch (error) {
    return false;
  }
}

function respectsLimit(response) {
  try {
    return (response.json('data.comments') || []).length <= LIMIT;
  } catch (error) {
    return false;
  }
}

function commentsUrl(cursor) {
  const cursorQuery = cursor ? `&cursor=${encodeURIComponent(cursor)}` : '';
  return `${BASE_URL}/api/${VERSION}/comments?postId=${POST_ID}&limit=${LIMIT}${cursorQuery}`;
}

export function setup() {
  let cursor = '';
  for (let index = 0; index < CURSOR_STEPS; index += 1) {
    const response = http.get(commentsUrl(cursor), { headers: headers(), tags: { name: `comment_prepare_${VERSION}` } });
    if (!apiOk(response)) exec.test.abort(`cursor 준비 실패: ${response.status} ${response.body}`);
    cursor = response.json('data.nextCursor') || '';
    if (!cursor) exec.test.abort(`CURSOR_STEPS=${CURSOR_STEPS} 전에 마지막 페이지에 도달했습니다.`);
  }
  return { cursor };
}

export function readPage(data) {
  const requestHeaders = headers();
  const responses = http.batch([
    ['GET', `${BASE_URL}/api/v1/posts/${POST_ID}`, null, {
      headers: requestHeaders,
      tags: { name: 'post_detail' },
    }],
    ['GET', commentsUrl(data.cursor), null, {
      headers: requestHeaders,
      tags: { name: `comment_page_${VERSION}` },
    }],
  ]);
  const postResponse = responses[0];
  const commentResponse = responses[1];
  const commentsOk = check(commentResponse, {
    'comment ApiResponse success': apiOk,
    'comment response respects limit': respectsLimit,
  });
  const postOk = check(postResponse, {
    'post detail ApiResponse success': apiOk,
  });
  const ok = commentsOk && postOk;

  commentDuration.add(commentResponse.timings.duration);
  detailScreenDuration.add(Math.max(postResponse.timings.duration, commentResponse.timings.duration));
  requestSuccess.add(ok);
}
