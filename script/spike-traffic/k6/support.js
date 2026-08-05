const LOOPBACK_HTTP = /^http:\/\/(?:localhost|127\.0\.0\.1|\[::1\])(?::\d+)?$/i;
const HTTPS = /^https:\/\/[^/?#]+(?::\d+)?$/i;

export function readBaseUrl() {
  const baseUrl = (__ENV.BASE_URL || 'http://localhost:8080').replace(/\/+$/, '');
  if (!LOOPBACK_HTTP.test(baseUrl) && !HTTPS.test(baseUrl)) {
    throw new Error('BASE_URL은 loopback HTTP 또는 HTTPS 주소여야 합니다.');
  }
  if (!LOOPBACK_HTTP.test(baseUrl) && __ENV.CONFIRM_LOAD_TEST !== '1') {
    throw new Error('원격 부하 테스트에는 CONFIRM_LOAD_TEST=1이 필요합니다. 대상 환경을 다시 확인하세요.');
  }
  return baseUrl;
}

export function readPositiveIds(name, fallback = '') {
  const raw = __ENV[name] || fallback;
  const values = raw.split(',').map((value) => value.trim()).filter(Boolean);
  if (values.length === 0 || values.some((value) => !/^[1-9]\d*$/.test(value))) {
    throw new Error(`${name}에는 쉼표로 구분한 양의 ID가 필요합니다.`);
  }
  return values.map(Number);
}

export function randomItem(values) {
  return values[Math.floor(Math.random() * values.length)];
}

export function pickPost(postIds, hotPostCount, hotPostShare) {
  const hotCount = Math.min(Math.max(hotPostCount, 1), postIds.length);
  const candidates = Math.random() < hotPostShare ? postIds.slice(0, hotCount) : postIds.slice(hotCount);
  return randomItem(candidates.length > 0 ? candidates : postIds);
}

export function authenticatedParams(memberId, tags, contentType = false) {
  const headers = {
    Accept: 'application/json',
    Authorization: `Bearer ${memberId}`,
  };
  if (contentType) headers['Content-Type'] = 'application/json';
  return { headers, redirects: 0, tags };
}

export function apiResponseOk(response, expectedStatus) {
  if (response.status !== expectedStatus) return false;
  try {
    return Number(response.json('code')) === expectedStatus;
  } catch (error) {
    return false;
  }
}

export function envNumber(name, fallback, minimum = 0) {
  const value = Number(__ENV[name] || fallback);
  if (!Number.isFinite(value) || value < minimum) {
    throw new Error(`${name}은 ${minimum} 이상의 숫자여야 합니다.`);
  }
  return value;
}

export function envInteger(name, fallback, minimum = 0) {
  const value = envNumber(name, fallback, minimum);
  if (!Number.isInteger(value)) throw new Error(`${name}은 정수여야 합니다.`);
  return value;
}

export function seconds(value) {
  return `${value}s`;
}
