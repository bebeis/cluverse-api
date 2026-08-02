import http from 'k6/http';
import exec from 'k6/execution';
import { check } from 'k6';
import { Rate } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const POST_ID = Number(__ENV.POST_ID || 0);
const AUTH_TOKEN = __ENV.AUTH_TOKEN || '';
const LIMIT = Number(__ENV.LIMIT || 100);
const MAX_PAGES = Number(__ENV.MAX_PAGES || 100);

if (!POST_ID) throw new Error('POST_ID가 필요합니다.');

export const options = {
  scenarios: {
    correctness: { executor: 'shared-iterations', vus: 1, iterations: 1 },
  },
  thresholds: { comment_page_equivalence: ['rate==1'] },
  tags: { version: 'comparison', kind: 'correctness' },
};

const equivalence = new Rate('comment_page_equivalence');

function headers() {
  return AUTH_TOKEN
    ? { Accept: 'application/json', Authorization: `Bearer ${AUTH_TOKEN}` }
    : { Accept: 'application/json' };
}

function parseSuccessPayload(response, version) {
  const contentType = String(response.headers['Content-Type'] || '').toLowerCase();
  if (response.status !== 200 || !contentType.includes('application/json')) {
    exec.test.abort(`${version} 조회 실패: ${response.status} ${response.body}`);
  }
  try {
    const payload = response.json();
    if (Number(payload.code) !== 200) {
      exec.test.abort(`${version} 조회 실패: ${response.status} ${response.body}`);
    }
    return payload;
  } catch (error) {
    exec.test.abort(`${version} 조회 실패: ${response.status} ${response.body}`);
    return null;
  }
}

function readAll(version) {
  const ids = [];
  const seen = new Set();
  let cursor = '';
  for (let page = 0; page < MAX_PAGES; page += 1) {
    const cursorQuery = cursor ? `&cursor=${encodeURIComponent(cursor)}` : '';
    const response = http.get(
      `${BASE_URL}/api/${version}/comments?postId=${POST_ID}&limit=${LIMIT}${cursorQuery}`,
      { headers: headers(), tags: { name: `comment_correctness_${version}` } },
    );
    const payload = parseSuccessPayload(response, version);
    const comments = payload.data.comments || [];
    for (const comment of comments) {
      if (seen.has(comment.commentId)) exec.test.abort(`${version} 중복 댓글: ${comment.commentId}`);
      seen.add(comment.commentId);
      ids.push(comment.commentId);
    }
    if (!payload.data.hasNext) return ids;
    cursor = payload.data.nextCursor || '';
    if (!cursor) exec.test.abort(`${version} hasNext=true인데 nextCursor가 없습니다.`);
  }
  exec.test.abort(`${version} MAX_PAGES=${MAX_PAGES}를 초과했습니다.`);
  return ids;
}

export default function () {
  const beforeIds = readAll('v1');
  const afterIds = readAll('v2');
  const same = beforeIds.length === afterIds.length
    && beforeIds.every((commentId, index) => commentId === afterIds[index]);
  equivalence.add(same);
  check({ same }, { 'before and after pages are equivalent': (value) => value.same });
  if (!same) exec.test.abort(`조회 결과 불일치: before=${beforeIds.length}, after=${afterIds.length}`);
}
