#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

PROFILE="${1:-}"
case "$PROFILE" in
  warm|cold-burst) ;;
  *) echo "사용법: run.sh warm|cold-burst [k6 인자...]" >&2; exit 1 ;;
esac
shift

: "${MEMBER_ID:?MEMBER_ID가 필요합니다. 예: MEMBER_ID=1}"
: "${BENCHMARK_TOKEN:?BENCHMARK_TOKEN이 필요합니다.}"
command -v curl >/dev/null || { echo "curl이 필요합니다." >&2; exit 1; }
command -v jq >/dev/null || { echo "jq가 필요합니다." >&2; exit 1; }
command -v k6 >/dev/null || { echo "k6가 필요합니다." >&2; exit 1; }

BASE_URL="${BASE_URL:-http://localhost:8080}"
STUB_URL="${STUB_URL:-http://127.0.0.1:19091}"

READINESS="$(curl --fail --silent --show-error \
  -H "X-Benchmark-Token: ${BENCHMARK_TOKEN}" \
  "${BASE_URL}/api/v1/certification/benchmark-readiness")"
if [[ "$(jq -r '.data.providerMode' <<<"$READINESS")" != "STUB" \
   || "$(jq -r '.data.experimentEndpointsEnabled' <<<"$READINESS")" != "true" \
   || "$(jq -r '.data.stubProvider' <<<"$READINESS")" != "true" ]]; then
  echo "중단: STUB provider와 실험 API가 확인되지 않았습니다." >&2
  exit 2
fi

curl --fail --silent --show-error \
  -H 'Content-Type: application/json' \
  --data '{"delayMs":100,"status":200,"responseMode":"valid"}' \
  "${STUB_URL}/_control" >/dev/null
curl --fail --silent --show-error --request POST "${STUB_URL}/_reset" >/dev/null

mkdir -p results/raw
STAMP="$(date +%F-%H%M%S)"
RUN_DIR="results/raw/${STAMP}-${PROFILE}"
mkdir -p "$RUN_DIR"
SUMMARY="${RUN_DIR}/k6-summary.json"
CONSOLE="${RUN_DIR}/k6-console.txt"
METRICS="${RUN_DIR}/provider-metrics.json"

K6_ENV=(
  -e "PROFILE=${PROFILE}"
  -e "BASE_URL=${BASE_URL}"
  -e "MEMBER_ID=${MEMBER_ID}"
  -e "BENCHMARK_TOKEN=${BENCHMARK_TOKEN}"
)
for name in RATE DURATION BURST_VUS PRE_ALLOCATED_VUS MAX_VUS; do
  if [[ -n "${!name:-}" ]]; then
    K6_ENV+=(-e "${name}=${!name}")
  fi
done

set +e
k6 run --summary-export "$SUMMARY" "${K6_ENV[@]}" "$@" \
  k6/certification-deadlines.k6.js 2>&1 | tee "$CONSOLE"
K6_STATUS=${PIPESTATUS[0]}
set -e

curl --fail --silent --show-error "${STUB_URL}/_metrics" | jq . > "$METRICS"
PROVIDER_CALLS="$(jq -r '.calls' "$METRICS")"
if [[ "$PROVIDER_CALLS" != "2" ]]; then
  echo "실패: 현재 연도와 다음 연도 외부 호출은 총 2회여야 하지만 ${PROVIDER_CALLS}회였습니다." >&2
  exit 3
fi
if [[ "$K6_STATUS" -ne 0 ]]; then
  echo "k6 threshold 실패. 결과는 보존했습니다: script/certification/${RUN_DIR}" >&2
  exit "$K6_STATUS"
fi

echo "통과: ${PROFILE}, provider 호출 ${PROVIDER_CALLS}회"
echo "결과: script/certification/${RUN_DIR}"
