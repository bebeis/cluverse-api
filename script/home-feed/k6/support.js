const LOOPBACK_HTTP = /^http:\/\/(?:localhost|127\.0\.0\.1|\[::1\])(?::\d+)?$/i;
const HTTPS = /^https:\/\/[^/?#]+(?::\d+)?$/i;

export function readBaseUrl() {
  const baseUrl = (__ENV.BASE_URL || 'http://localhost:8080').replace(/\/+$/, '');
  if (!LOOPBACK_HTTP.test(baseUrl) && !HTTPS.test(baseUrl)) {
    throw new Error('BASE_URL은 loopback HTTP 또는 HTTPS 주소여야 합니다.');
  }
  return baseUrl;
}

export function readSessionCookie() {
  const sessionCookie = __ENV.SESSION_COOKIE || '';
  if (!sessionCookie) {
    throw new Error('SESSION_COOKIE이 필요합니다. 예: JSESSIONID=...');
  }
  return sessionCookie;
}

export function authenticatedParams(sessionCookie, tags, contentType = false) {
  const headers = {
    Accept: 'application/json',
    Cookie: sessionCookie,
  };
  if (contentType) {
    headers['Content-Type'] = 'application/json';
  }
  return {
    headers,
    redirects: 0,
    tags,
  };
}
