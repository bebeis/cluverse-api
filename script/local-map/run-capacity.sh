#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

VERSION="${1:-}"
case "$VERSION" in
  v1|v2) ;;
  *)
    echo "사용법: run-capacity.sh v1|v2" >&2
    exit 1
    ;;
esac

: "${AUTH_TOKEN:?AUTH_TOKEN이 필요합니다.}"
: "${BENCHMARK_TOKEN:?BENCHMARK_TOKEN이 필요합니다.}"

START_RATE="${START_RATE:-20}"
STEP_RATE="${STEP_RATE:-10}"
MAX_RATE="${MAX_RATE:-200}"
REPETITIONS="${REPETITIONS:-3}"
STUB_DELAY_MS="${STUB_DELAY_MS:-300}"
DURATION="${DURATION:-30s}"
COOLDOWN_SECONDS="${COOLDOWN_SECONDS:-15}"

for value in "$START_RATE" "$STEP_RATE" "$MAX_RATE" "$REPETITIONS"; do
  [[ "$value" =~ ^[0-9]+$ && "$value" -gt 0 ]] || {
    echo "START_RATE, STEP_RATE, MAX_RATE, REPETITIONS는 양의 정수여야 합니다." >&2
    exit 2
  }
done
[[ "$COOLDOWN_SECONDS" =~ ^[0-9]+$ ]] || {
  echo "COOLDOWN_SECONDS는 0 이상의 정수여야 합니다." >&2
  exit 2
}
(( MAX_RATE >= START_RATE )) || { echo "MAX_RATE는 START_RATE 이상이어야 합니다." >&2; exit 2; }

metric_value() {
  local file="$1"
  local metric="$2"
  local stat="$3"
  jq -r --arg metric "$metric" --arg stat "$stat" \
    '.metrics[$metric].values[$stat]
      // .metrics[$metric][$stat]
      // (if $stat == "rate" then .metrics[$metric].value else empty end)
      // empty' "$file"
}

LAST_PASS=""
RATE="$START_RATE"
CAPACITY_STAMP="$(date +%F-%H%M%S)"
CAPACITY_CSV="${CAPACITY_CSV:-results/${CAPACITY_STAMP}-${VERSION}-capacity-delay-${STUB_DELAY_MS}ms.csv}"
mkdir -p "$(dirname "$CAPACITY_CSV")"
printf '%s\n' 'version,stub_delay_ms,offered_rps,repetition,p99_ms,success_rate,dropped_iterations,status,summary_file' > "$CAPACITY_CSV"

while (( RATE <= MAX_RATE )); do
  echo "측정 시작: version=${VERSION}, delay=${STUB_DELAY_MS}ms, offered=${RATE} RPS"
  PASSED=1

  for (( repetition=1; repetition<=REPETITIONS; repetition++ )); do
    STAMP="$(date +%F-%H%M%S)"
    SUMMARY_FILE="results/raw/${STAMP}-${VERSION}-capacity-delay-${STUB_DELAY_MS}ms-rate-${RATE}-r${repetition}-summary.json"
    RUN_PRE_ALLOCATED_VUS="${PRE_ALLOCATED_VUS:-$(( (RATE * 12 + 9) / 10 ))}"
    RUN_MAX_VUS="${MAX_VUS:-$(( RATE * 2 ))}"

    set +e
    RATE="$RATE" DURATION="$DURATION" STUB_DELAY_MS="$STUB_DELAY_MS" SUMMARY_FILE="$SUMMARY_FILE" \
      PRE_ALLOCATED_VUS="$RUN_PRE_ALLOCATED_VUS" MAX_VUS="$RUN_MAX_VUS" \
      ./run.sh "$VERSION"
    STATUS=$?
    set -e

    P99="$(metric_value "$SUMMARY_FILE" local_map_write_duration 'p(99)')"
    SUCCESS="$(metric_value "$SUMMARY_FILE" local_map_write_success rate)"
    DROPPED="$(metric_value "$SUMMARY_FILE" dropped_iterations count)"
    echo "반복 ${repetition}: p99=${P99:-N/A}ms success=${SUCCESS:-N/A} dropped=${DROPPED:-0} status=${STATUS}"
    printf '%s,%s,%s,%s,%s,%s,%s,%s,%s\n' \
      "$VERSION" "$STUB_DELAY_MS" "$RATE" "$repetition" "${P99:-}" "${SUCCESS:-}" \
      "${DROPPED:-0}" "$STATUS" "$SUMMARY_FILE" >> "$CAPACITY_CSV"

    if (( STATUS != 0 )); then
      PASSED=0
      break
    fi
    if (( repetition < REPETITIONS && COOLDOWN_SECONDS > 0 )); then
      sleep "$COOLDOWN_SECONDS"
    fi
  done

  if (( PASSED == 0 )); then
    echo "SLO 중단점: ${RATE} RPS (p99 < 1000ms, success >= 99%, dropped = 0 중 하나 이상 실패)"
    if [[ -n "$LAST_PASS" ]]; then
      echo "최대 통과 RPS: ${LAST_PASS}"
    else
      echo "${START_RATE} RPS 이상에서 통과한 구간이 없습니다."
    fi
    echo "capacity CSV: script/local-map/${CAPACITY_CSV}"
    exit 0
  fi

  LAST_PASS="$RATE"
  RATE=$(( RATE + STEP_RATE ))
done

echo "설정한 MAX_RATE=${MAX_RATE}까지 모두 통과했습니다. 최대 RPS를 높여 다시 실행하세요."
echo "capacity CSV: script/local-map/${CAPACITY_CSV}"
