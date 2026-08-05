import http from 'k6/http';
import { check } from 'k6';

const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';
const version = (__ENV.VERSION || 'v4').toLowerCase();
const postId = Number(__ENV.POST_ID || 5999999);

if (!['v2', 'v3', 'v4'].includes(version)) throw new Error('VERSION은 v2, v3, v4 중 하나여야 합니다.');
export const options = {
  vus: 1,
  iterations: 1,
  summaryTrendStats: ['avg', 'p(95)', 'p(99)', 'max'],
};

function count(cookieId) {
  return http.post(`${baseUrl}/api/${version}/posts/${postId}/view-count`, null, {
    headers: { Cookie: `cluverse_viewer=${cookieId}` },
  });
}

export default function () {
  const duplicateCookie = `correctness-duplicate-${Date.now()}`;
  const first = count(duplicateCookie);
  const second = count(duplicateCookie);
  check(first, { 'first request is counted': (res) => res.status === 200 && res.json('data.counted') === true });
  check(second, {
    'duplicate request is rejected': (res) => res.status === 200 && res.json('data.counted') === false,
    'duplicate keeps the same visible count': (res) => res.json('data.viewCount') === first.json('data.viewCount'),
  });
  if (version === 'v4') {
    let previous = Number(second.json('data.viewCount'));
    for (let index = 0; index < 20; index += 1) {
      const response = count(`correctness-monotonic-${Date.now()}-${index}`);
      const current = Number(response.json('data.viewCount'));
      check(response, { 'v4 visible count never decreases': () => current >= previous });
      previous = current;
    }
  }
}
