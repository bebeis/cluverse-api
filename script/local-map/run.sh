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
RATE="${RATE:-20}"
DURATION="${DURATION:-30s}"
PRE_ALLOCATED_VUS="${PRE_ALLOCATED_VUS:-100}"
MAX_VUS="${MAX_VUS:-250}"
STUB_DELAY_MS="${STUB_DELAY_MS:-300}"

command -v jq >/dev/null || { echo "jq가 필요합니다." >&2; exit 1; }

if [[ "$KIND" == "v1" || "$KIND" == "v2" ]]; then
  [[ "$RATE" =~ ^[0-9]+$ && "$RATE" -gt 0 ]] || {
    echo "RATE는 양의 정수여야 합니다." >&2
    exit 2
  }
  [[ "$STUB_DELAY_MS" =~ ^[0-9]+$ && "$STUB_DELAY_MS" -le 10000 ]] || {
    echo "STUB_DELAY_MS는 0~10000 정수여야 합니다." >&2
    exit 2
  }
fi

READINESS="$(curl --fail --silent --show-error \
  -H "X-Benchmark-Token: ${BENCHMARK_TOKEN}" \
  "${BASE_URL}/api/v1/local-map/benchmark-readiness")"
if [[ "$READINESS" != *'"providerMode":"STUB"'* \
   || "$READINESS" != *'"experimentEndpointsEnabled":true'* \
   || "$READINESS" != *'"stubProvider":true'* ]]; then
  echo "중단: STUB provider와 실험 API가 확인되지 않았습니다." >&2
  exit 2
fi

if [[ "$KIND" == "v1" || "$KIND" == "v2" ]]; then
  RESET_RESPONSE="$(curl --fail --silent --show-error --request POST \
    -H "X-Benchmark-Token: ${BENCHMARK_TOKEN}" \
    "${BASE_URL}/api/v1/local-map/benchmark-stub/reset?delayMillis=${STUB_DELAY_MS}")"
  [[ "$(jq -r '.data.stubDelayMillis' <<<"$RESET_RESPONSE")" == "$STUB_DELAY_MS" \
     && "$(jq -r '.data.stubSearchCalls' <<<"$RESET_RESPONSE")" == "0" ]] || {
    echo "provider mock 초기화 상태를 확인할 수 없습니다." >&2
    exit 2
  }
fi

mkdir -p results/raw
STAMP="$(date +%F-%H%M%S)"
SUMMARY="${SUMMARY_FILE:-results/raw/${STAMP}-${KIND}-delay-${STUB_DELAY_MS}ms-rate-${RATE}-summary.json}"
HTML="${HTML_FILE:-${SUMMARY%-summary.json}.html}"
export BASE_URL AUTH_TOKEN BENCHMARK_TOKEN

K6_ENV=(
  -e "BASE_URL=${BASE_URL}"
  -e "AUTH_TOKEN=${AUTH_TOKEN}"
  -e "BENCHMARK_TOKEN=${BENCHMARK_TOKEN}"
)
for name in BOARD_ID QUERY RATE DURATION PRE_ALLOCATED_VUS MAX_VUS STUB_DELAY_MS; do
  if [[ -n "${!name:-}" ]]; then
    K6_ENV+=(-e "${name}=${!name}")
  fi
done

if [[ "$KIND" == "v1" || "$KIND" == "v2" ]]; then
  export VERSION="$KIND"
  K6_ENV+=(-e "VERSION=${VERSION}")
fi

set +e
K6_WEB_DASHBOARD=true K6_WEB_DASHBOARD_EXPORT="$HTML" \
  k6 run --summary-export "$SUMMARY" "${K6_ENV[@]}" "$@" "$K6_SCRIPT"
K6_STATUS=$?
set -e

if [[ "$KIND" == "v1" || "$KIND" == "v2" ]]; then
  STUB_METRICS="${SUMMARY%-summary.json}-stub.json"
  if [[ "$KIND" == "v2" ]]; then
    # V2 응답 뒤 provider 작업이 비동기로 끝날 시간을 준다. 이 대기는 k6 응답시간에 포함되지 않는다.
    ITERATIONS="$(jq -r '.metrics.iterations.values.count // .metrics.iterations.count // 0' "$SUMMARY")"
    ASYNC_EXPECTED_CALLS="$((ITERATIONS + 1))"
    for _ in {1..30}; do
      CURRENT_CALLS="$(curl --fail --silent --show-error \
        -H "X-Benchmark-Token: ${BENCHMARK_TOKEN}" \
        "${BASE_URL}/api/v1/local-map/benchmark-readiness" | jq -r '.data.stubSearchCalls')"
      if (( CURRENT_CALLS >= ASYNC_EXPECTED_CALLS )); then
        sleep "$(( (STUB_DELAY_MS + 999) / 1000 + 1 ))"
        break
      fi
      sleep 1
    done
  fi
  curl --fail --silent --show-error \
    -H "X-Benchmark-Token: ${BENCHMARK_TOKEN}" \
    --output "$STUB_METRICS" "${BASE_URL}/api/v1/local-map/benchmark-readiness"
  if [[ "$KIND" == "v2" ]]; then
    OBSERVED_CALLS="$(jq -r '.data.stubSearchCalls' "$STUB_METRICS")"
    if [[ "$OBSERVED_CALLS" -lt "$ASYNC_EXPECTED_CALLS" ]]; then
      echo "실패: V2 비동기 작업이 측정 종료 후 30초 안에 provider까지 전달되지 않았습니다. expected=${ASYNC_EXPECTED_CALLS} observed=${OBSERVED_CALLS}" >&2
      K6_STATUS=3
    fi
  fi
  echo "stub metrics: script/local-map/${STUB_METRICS}"
fi
echo "summary JSON: script/local-map/${SUMMARY}"
echo "dashboard HTML: script/local-map/${HTML}"
exit "$K6_STATUS"
