import http from 'k6/http';
import { check } from 'k6';
import exec from 'k6/execution';
import { Counter, Rate, Trend } from 'k6/metrics';

const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';
const condition = (__ENV.CONDITION || '').toLowerCase();
const kind = (__ENV.KIND || '').toLowerCase();
const memberId = Number(__ENV.MEMBER_ID || 9200000000);
const rate = Number(__ENV.RATE || 5);
const duration = __ENV.DURATION || '60s';
const postOffset = Number(__ENV.POST_OFFSET || 0);
const postsPerGroup = Number(__ENV.POSTS_PER_GROUP || 1000);

if (!['disabled', 'enabled'].includes(condition)) {
  throw new Error('CONDITION은 disabled 또는 enabled여야 합니다.');
}
if (!['like', 'comment'].includes(kind)) {
  throw new Error('KIND는 like 또는 comment여야 합니다.');
}

const groupStarts = {
  'disabled-like': 9200000001,
  'enabled-like': 9200001001,
  'disabled-comment': 9200002001,
  'enabled-comment': 9200003001,
};
const postIdMin = groupStarts[`${condition}-${kind}`];

function durationSeconds(text) {
  const match = String(text).match(/^(\d+)(s|m)$/);
  if (!match) throw new Error(`DURATION은 정수 s 또는 m 단위여야 합니다: ${text}`);
  return Number(match[1]) * (match[2] === 'm' ? 60 : 1);
}

const expectedIterations = rate * durationSeconds(duration);
if (postOffset < 0 || postOffset + expectedIterations > postsPerGroup) {
  throw new Error(`게시글 범위를 넘습니다: offset=${postOffset}, expected=${expectedIterations}`);
}

export const options = {
  scenarios: {
    actual_api: {
      executor: 'constant-arrival-rate',
      rate,
      timeUnit: '1s',
      duration,
      preAllocatedVUs: Number(__ENV.PRE_ALLOCATED_VUS || 5),
      maxVUs: Number(__ENV.MAX_VUS || 20),
    },
  },
  thresholds: {
    popularity_inline_failures: ['rate<0.01'],
    dropped_iterations: ['count==0'],
  },
  summaryTrendStats: ['avg', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
  tags: { condition, kind },
};

const apiDuration = new Trend('popularity_inline_api_duration', true);
const failures = new Rate('popularity_inline_failures');
const completed = new Counter('popularity_inline_completed');

function headers() {
  return {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${memberId}`,
  };
}

export default function () {
  const postId = postIdMin + postOffset + exec.scenario.iterationInTest;
  const response = kind === 'like'
    ? http.post(`${baseUrl}/api/v1/posts/${postId}/likes`, null, {
        headers: headers(), tags: { name: 'popularity_inline_like' },
      })
    : http.post(
        `${baseUrl}/api/v1/comments?postId=${postId}`,
        JSON.stringify({
          parentCommentId: null,
          content: `popularity-inline:${condition}:${postId}`,
          isAnonymous: false,
        }),
        { headers: headers(), tags: { name: 'popularity_inline_comment' } },
      );

  const successful = check(response, {
    'actual API returns 201': (value) => value.status === 201,
    'ApiResponse code is 201': (value) => {
      try { return Number(value.json('code')) === 201; } catch (_) { return false; }
    },
  });
  apiDuration.add(response.timings.duration);
  failures.add(!successful);
  if (successful) completed.add(1);
}
