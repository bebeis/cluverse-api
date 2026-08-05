const LOOPBACK_HTTP = /^http:\/\/(?:localhost|127\.0\.0\.1|\[::1\])(?::\d+)?$/i;
const HTTPS = /^https:\/\/[^/?#]+(?::\d+)?$/i;

export function readBaseUrl() {
  const baseUrl = (__ENV.BASE_URL || 'http://localhost:8080').replace(/\/+$/, '');
  if (!LOOPBACK_HTTP.test(baseUrl) && !HTTPS.test(baseUrl)) {
    throw new Error('BASE_URL은 loopback HTTP 또는 HTTPS 주소여야 합니다.');
  }
  return baseUrl;
}

export function readMemberId() {
  const memberId = __ENV.MEMBER_ID || '';
  if (!/^[1-9]\d*$/.test(memberId)) {
    throw new Error('MEMBER_ID must be a positive number. Example: MEMBER_ID=1');
  }
  return memberId;
}

export function authenticatedParams(memberId, tags, contentType = false) {
  const headers = {
    Accept: 'application/json',
    Authorization: `Bearer ${memberId}`,
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
