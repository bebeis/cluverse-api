#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

: "${MEMBER_ID:?MEMBER_ID가 필요합니다. 예: MEMBER_ID=1}"
: "${BENCHMARK_TOKEN:?BENCHMARK_TOKEN이 필요합니다.}"
command -v curl >/dev/null || { echo "curl이 필요합니다." >&2; exit 1; }
command -v jq >/dev/null || { echo "jq가 필요합니다." >&2; exit 1; }

BASE_URL="${BASE_URL:-http://localhost:8080}"
STUB_URL="${STUB_URL:-http://127.0.0.1:19091}"
BODY_FILE="$(mktemp)"

cleanup() {
  curl --silent --request POST \
    -H 'Content-Type: application/json' \
    --data '{"delayMs":100,"status":200,"responseMode":"valid"}' \
    "${STUB_URL}/_control" >/dev/null 2>&1 || true
  rm -f -- "$BODY_FILE"
}
trap cleanup EXIT

READINESS="$(curl --fail --silent --show-error \
  -H "X-Benchmark-Token: ${BENCHMARK_TOKEN}" \
  "${BASE_URL}/api/v1/certification/benchmark-readiness")"
if [[ "$(jq -r '.data.providerMode' <<<"$READINESS")" != "STUB" \
   || "$(jq -r '.data.experimentEndpointsEnabled' <<<"$READINESS")" != "true" \
   || "$(jq -r '.data.stubProvider' <<<"$READINESS")" != "true" ]]; then
  echo "중단: STUB provider와 실험 API가 확인되지 않았습니다." >&2
  exit 2
fi

evict_cache() {
  curl --fail --silent --show-error --request DELETE \
    -H "X-Benchmark-Token: ${BENCHMARK_TOKEN}" \
    "${BASE_URL}/api/v1/certification/benchmark-cache" >/dev/null
}

configure_stub() {
  curl --fail --silent --show-error \
    -H 'Content-Type: application/json' \
    --data "$1" \
    "${STUB_URL}/_control" >/dev/null
}

expect_status() {
  local expected="$1"
  local label="$2"
  local status
  evict_cache
  status="$(curl --silent --show-error --output "$BODY_FILE" --write-out '%{http_code}' \
    -H "Authorization: Bearer ${MEMBER_ID}" \
    "${BASE_URL}/api/v1/home/certification-deadlines")"
  if [[ "$status" != "$expected" ]]; then
    echo "실패: ${label} 응답은 HTTP ${expected}여야 하지만 ${status}였습니다." >&2
    exit 3
  fi
  echo "통과: ${label} -> HTTP ${status}"
}

configure_stub '{"delayMs":0,"status":200,"responseMode":"provider-error"}'
expect_status 502 "공급자 오류 코드"

configure_stub '{"delayMs":0,"status":200,"responseMode":"malformed"}'
expect_status 502 "깨진 JSON"

configure_stub '{"delayMs":2500,"status":200,"responseMode":"valid"}'
expect_status 502 "읽기 타임아웃"
