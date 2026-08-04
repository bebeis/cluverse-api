import http from 'k6/http';
import { check } from 'k6';
import { Rate, Trend } from 'k6/metrics';
import { authenticatedParams, readBaseUrl, readSessionCookie } from './support.js';

const VERSION = (__ENV.VERSION || '').toLowerCase();
const BASE_URL = readBaseUrl();
const SESSION_COOKIE = readSessionCookie();
const RATE = Number(__ENV.RATE || 20);
const DURATION = __ENV.DURATION || '30s';
const COMMENTS = __ENV.COMMENTS || 'unknown';
const COMMENTED_POSTS = __ENV.COMMENTED_POSTS || 'unknown';
const HOT_COMMENT_PERCENT = __ENV.HOT_COMMENT_PERCENT || 'unknown';

if (!['v1', 'v2', 'v3'].includes(VERSION)) throw new Error('VERSION은 v1, v2, v3 중 하나여야 합니다.');
export const options = {
  scenarios: {
    read: {
      executor: 'constant-arrival-rate',
      rate: RATE,
      timeUnit: '1s',
      duration: DURATION,
      preAllocatedVUs: Number(__ENV.PRE_ALLOCATED_VUS || 20),
      maxVUs: Number(__ENV.MAX_VUS || 100),
    },
  },
  thresholds: {
    dropped_iterations: ['count==0'],
    home_recent_posts_success: ['rate>0.99'],
    home_recent_posts_duration: ['p(95)<3000'],
  },
  summaryTrendStats: ['avg', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
  tags: {
    version: VERSION,
    comments: COMMENTS,
    commented_posts: COMMENTED_POSTS,
    hot_comment_percent: HOT_COMMENT_PERCENT,
  },
};

const duration = new Trend('home_recent_posts_duration', true);
const success = new Rate('home_recent_posts_success');

export function setup() {
  if (VERSION !== 'v2') return;
  const response = http.get(
    `${BASE_URL}/api/${VERSION}/home/recent-commented-posts`,
    authenticatedParams(SESSION_COOKIE, { name: 'home_recent_commented_posts_v2_warmup' }),
  );
  if (response.status !== 200) throw new Error(`V2 캐시 예열 실패: ${response.status}`);
}

export default function () {
  const response = http.get(
    `${BASE_URL}/api/${VERSION}/home/recent-commented-posts`,
    authenticatedParams(SESSION_COOKIE, { name: `home_recent_commented_posts_${VERSION}` }),
  );
  const ok = check(response, {
    'home recent posts success': (value) => {
      if (value.status !== 200) return false;
      try {
        const payload = value.json();
        return Number(payload.code) === 200
          && Array.isArray(payload.data)
          && payload.data.length <= 10;
      } catch (error) {
        return false;
      }
    },
  });
  duration.add(response.timings.duration);
  success.add(ok);
}
