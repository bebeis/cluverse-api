#!/usr/bin/env bash
set -euo pipefail

: "${SESSION_COOKIE:?SESSION_COOKIE이 필요합니다. 예: JSESSIONID=...}"
BASE_URL="${BASE_URL:-http://localhost:8080}"

curl --fail --silent --show-error \
  -H "Accept: application/json" \
  -H "Cookie: ${SESSION_COOKIE}" \
  "${BASE_URL}/api/v1/home/recent-commented-posts"

curl --fail --silent --show-error \
  -H "Accept: application/json" \
  -H "Cookie: ${SESSION_COOKIE}" \
  "${BASE_URL}/api/v2/home/recent-commented-posts"

curl --fail --silent --show-error \
  -H "Accept: application/json" \
  -H "Cookie: ${SESSION_COOKIE}" \
  "${BASE_URL}/api/v3/home/recent-commented-posts"
