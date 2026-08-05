import http from 'k6/http';
import { check } from 'k6';
import { Rate, Trend } from 'k6/metrics';
import { authenticatedParams, readBaseUrl, readMemberId } from './support.js';

const BASE_URL = readBaseUrl();
const MEMBER_ID = readMemberId();
const POST_ID = Number(__ENV.POST_ID || 0);
const RATE = Number(__ENV.RATE || 2);
const DURATION = __ENV.DURATION || '30s';
const RUN_ID = __ENV.RUN_ID || 'manual';

if (!POST_ID) throw new Error('POST_ID가 필요합니다.');

export const options = {
  scenarios: {
    write: {
      executor: 'constant-arrival-rate',
      rate: RATE,
      timeUnit: '1s',
      duration: DURATION,
      preAllocatedVUs: Number(__ENV.PRE_ALLOCATED_VUS || 5),
      maxVUs: Number(__ENV.MAX_VUS || 20),
    },
  },
  thresholds: {
    dropped_iterations: ['count==0'],
    home_comment_write_success: ['rate>0.99'],
    home_comment_write_duration: ['p(95)<3000'],
  },
  summaryTrendStats: ['avg', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
  tags: { version: 'v3', kind: 'write' },
};

const duration = new Trend('home_comment_write_duration', true);
const success = new Rate('home_comment_write_success');

export default function () {
  const response = http.post(
    `${BASE_URL}/api/v1/comments?postId=${POST_ID}`,
    JSON.stringify({
      parentCommentId: null,
      content: `home-feed-benchmark:${RUN_ID}`,
      isAnonymous: false,
    }),
    authenticatedParams(
      MEMBER_ID,
      { name: 'home_comment_write_projection' },
      true,
    ),
  );
  const ok = check(response, {
    'comment write success': (value) => {
      if (value.status !== 201) return false;
      try {
        return Number(value.json('code')) === 201;
      } catch (error) {
        return false;
      }
    },
  });
  duration.add(response.timings.duration);
  success.add(ok);
}
