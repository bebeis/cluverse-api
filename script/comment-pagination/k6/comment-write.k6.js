import http from 'k6/http';
import { check } from 'k6';
import { Rate, Trend } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const POST_ID = Number(__ENV.POST_ID || 0);
const AUTH_TOKEN = __ENV.AUTH_TOKEN || '';
const WRITE_KIND = __ENV.WRITE_KIND || 'root';
const PARENT_COMMENT_ID = __ENV.PARENT_COMMENT_ID ? Number(__ENV.PARENT_COMMENT_ID) : null;
const RATE = Number(__ENV.RATE || 2);
const DURATION = __ENV.DURATION || '30s';
const RUN_ID = __ENV.RUN_ID || 'manual';

if (!POST_ID) throw new Error('POST_ID가 필요합니다.');
if (!AUTH_TOKEN) throw new Error('AUTH_TOKEN이 필요합니다.');
if (WRITE_KIND === 'reply' && !PARENT_COMMENT_ID) throw new Error('답글 측정에는 PARENT_COMMENT_ID가 필요합니다.');

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
    comment_write_success: ['rate>0.99'],
    comment_write_duration: ['p(95)<3000'],
  },
  summaryTrendStats: ['avg', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
  tags: { version: 'v2', write_kind: WRITE_KIND },
};

const writeDuration = new Trend('comment_write_duration', true);
const writeSuccess = new Rate('comment_write_success');

export default function () {
  const body = JSON.stringify({
    parentCommentId: WRITE_KIND === 'reply' ? PARENT_COMMENT_ID : null,
    content: `comment-pagination-benchmark:${RUN_ID}:${WRITE_KIND}`,
    isAnonymous: false,
  });
  const response = http.post(`${BASE_URL}/api/v1/comments?postId=${POST_ID}`, body, {
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json',
      Authorization: `Bearer ${AUTH_TOKEN}`,
    },
    tags: { name: `comment_write_${WRITE_KIND}` },
  });
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
  writeDuration.add(response.timings.duration);
  writeSuccess.add(ok);
}
