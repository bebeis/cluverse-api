#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"
: "${AUTH_TOKEN:?AUTH_TOKEN이 필요합니다.}"
: "${BENCHMARK_TOKEN:?BENCHMARK_TOKEN이 필요합니다.}"
BASE_URL="${BASE_URL:-http://localhost:8080}"
QUERY="${QUERY:-연세대 카페}"

command -v jq >/dev/null || { echo "jq가 필요합니다." >&2; exit 1; }

READINESS="$(curl --fail --silent --show-error \
  -H "X-Benchmark-Token: ${BENCHMARK_TOKEN}" \
  "${BASE_URL}/api/v1/local-map/benchmark-readiness")"
[[ "$(jq -r '.data.providerMode' <<<"$READINESS")" == "STUB" ]] || { echo "STUB이 아닙니다." >&2; exit 2; }
[[ "$(jq -r '.data.stubProvider' <<<"$READINESS")" == "true" ]] || { echo "stubProvider=false" >&2; exit 2; }

V2_SEARCH="$(curl --fail --silent --show-error --get \
  -H "Authorization: Bearer ${AUTH_TOKEN}" \
  --data-urlencode "query=${QUERY}" \
  "${BASE_URL}/api/v2/places/search")"
jq -e '.data.places[0].selectionToken | length > 20' <<<"$V2_SEARCH" >/dev/null

V1_SEARCH="$(curl --fail --silent --show-error --get \
  -H "Authorization: Bearer ${AUTH_TOKEN}" \
  -H "X-Benchmark-Token: ${BENCHMARK_TOKEN}" \
  --data-urlencode "query=${QUERY}" \
  "${BASE_URL}/api/v1/places/search")"
jq -e '.data.places[0].sourceFingerprint | length == 64' <<<"$V1_SEARCH" >/dev/null

echo "통과: STUB 검색 응답, fingerprint, selectionToken 계약"
