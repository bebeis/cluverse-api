#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

KIND="${1:-}"
case "$KIND" in
  v1|v2) K6_SCRIPT="k6/local-map-write.k6.js" ;;
  correctness) K6_SCRIPT="k6/local-map-correctness.k6.js" ;;
  *)
    echo "사용법: run.sh v1|v2|correctness [k6 인자...]" >&2
    exit 1
    ;;
esac
shift

: "${BENCHMARK_TOKEN:?BENCHMARK_TOKEN이 필요합니다.}"
: "${AUTH_TOKEN:?AUTH_TOKEN이 필요합니다.}"
BASE_URL="${BASE_URL:-http://localhost:8080}"

READINESS="$(curl --fail --silent --show-error \
  -H "X-Benchmark-Token: ${BENCHMARK_TOKEN}" \
  "${BASE_URL}/api/v1/local-map/benchmark-readiness")"
if [[ "$READINESS" != *'"providerMode":"STUB"'* \
   || "$READINESS" != *'"experimentEndpointsEnabled":true'* \
   || "$READINESS" != *'"stubProvider":true'* ]]; then
  echo "중단: STUB provider와 실험 API가 확인되지 않았습니다." >&2
  exit 2
fi

mkdir -p results/raw
STAMP="$(date +%F-%H%M%S)"
SUMMARY="results/raw/${STAMP}-${KIND}-summary.json"
export BASE_URL AUTH_TOKEN BENCHMARK_TOKEN

K6_ENV=(
  -e "BASE_URL=${BASE_URL}"
  -e "AUTH_TOKEN=${AUTH_TOKEN}"
  -e "BENCHMARK_TOKEN=${BENCHMARK_TOKEN}"
)
for name in BOARD_ID QUERY RATE DURATION PRE_ALLOCATED_VUS MAX_VUS; do
  if [[ -n "${!name:-}" ]]; then
    K6_ENV+=(-e "${name}=${!name}")
  fi
done

if [[ "$KIND" == "v1" || "$KIND" == "v2" ]]; then
  export VERSION="$KIND"
  K6_ENV+=(-e "VERSION=${VERSION}")
fi

k6 run --summary-export "$SUMMARY" "${K6_ENV[@]}" "$@" "$K6_SCRIPT"
echo "summary JSON: script/local-map/${SUMMARY}"
