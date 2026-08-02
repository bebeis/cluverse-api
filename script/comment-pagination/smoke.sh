#!/usr/bin/env bash
set -euo pipefail

: "${POST_ID:?POST_ID가 필요합니다.}"
BASE_URL="${BASE_URL:-http://localhost:8080}"
AUTH_HEADER=()
if [[ -n "${AUTH_TOKEN:-}" ]]; then
  AUTH_HEADER=(-H "Authorization: Bearer ${AUTH_TOKEN}")
fi

for version in v1 v2; do
  RESPONSE="$(curl --fail --silent --show-error \
    "${AUTH_HEADER[@]}" \
    "${BASE_URL}/api/${version}/comments?postId=${POST_ID}&limit=3")"
  if [[ "$RESPONSE" != *'"code":200'* || "$RESPONSE" != *'"comments"'* ]]; then
    echo "${version} smoke 실패: ${RESPONSE}" >&2
    exit 1
  fi
  echo "${version} smoke 성공"
done
