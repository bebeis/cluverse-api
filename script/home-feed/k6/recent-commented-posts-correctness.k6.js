import http from 'k6/http';
import exec from 'k6/execution';
import { check } from 'k6';
import { Rate } from 'k6/metrics';
import { authenticatedParams, readBaseUrl, readSessionCookie } from './support.js';

const BASE_URL = readBaseUrl();
const SESSION_COOKIE = readSessionCookie();

export const options = {
  scenarios: {
    correctness: { executor: 'shared-iterations', vus: 1, iterations: 1 },
  },
  thresholds: { home_recent_posts_equivalence: ['rate==1'] },
  tags: { version: 'comparison', kind: 'correctness' },
};

const equivalence = new Rate('home_recent_posts_equivalence');

function read(version) {
  const response = http.get(
    `${BASE_URL}/api/${version}/home/recent-commented-posts`,
    authenticatedParams(SESSION_COOKIE, { name: `home_recent_posts_correctness_${version}` }),
  );
  const contentType = String(response.headers['Content-Type'] || '').toLowerCase();
  if (response.status !== 200 || !contentType.includes('application/json')) {
    exec.test.abort(`${version} 조회 실패: ${response.status} ${response.body}`);
  }
  try {
    const payload = response.json();
    if (Number(payload.code) !== 200 || !Array.isArray(payload.data)) {
      exec.test.abort(`${version} 응답 형식 오류: ${response.body}`);
    }
    return payload.data.map((post) => ({
      postId: Number(post.postId),
      title: post.title,
      lastCommentedAt: post.lastCommentedAt,
    }));
  } catch (error) {
    exec.test.abort(`${version} JSON 파싱 실패: ${response.body}`);
    return [];
  }
}

export default function () {
  const before = read('v1');
  const indexedAndCached = read('v2');
  const projected = read('v3');
  const same = JSON.stringify(before) === JSON.stringify(indexedAndCached)
    && JSON.stringify(before) === JSON.stringify(projected);
  equivalence.add(same);
  check({ same }, { 'three-stage results are equivalent': (value) => value.same });
  if (!same) {
    exec.test.abort(
      `조회 결과 불일치: before=${JSON.stringify(before)} indexedAndCached=${JSON.stringify(indexedAndCached)} projected=${JSON.stringify(projected)}`,
    );
  }
}
