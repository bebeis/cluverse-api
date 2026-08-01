#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

[[ "${CONFIRM_NAVER_CANARY:-}" == "YES" ]] || {
  echo "중단: CONFIRM_NAVER_CANARY=YES를 명시해야 합니다." >&2
  exit 2
}
[[ -z "${CI:-}" ]] || { echo "중단: CI에서는 실제 네이버 canary를 실행하지 않습니다." >&2; exit 2; }
: "${AUTH_TOKEN:?AUTH_TOKEN이 필요합니다.}"
command -v jq >/dev/null || { echo "jq가 필요합니다." >&2; exit 1; }

BASE_URL="${BASE_URL:-http://localhost:8080}"
QUERY="${QUERY:-연세대 카페}"
CALLS="${CALLS:-1}"
QPS="${QPS:-1}"
[[ "$CALLS" =~ ^[0-9]+$ && "$CALLS" -ge 1 && "$CALLS" -le 10 ]] || {
  echo "CALLS는 1~10이어야 합니다." >&2; exit 2;
}
[[ "$QPS" =~ ^[0-9]+$ && "$QPS" -ge 1 && "$QPS" -le 5 ]] || {
  echo "QPS는 1~5여야 합니다." >&2; exit 2;
}

LEDGER_DIR="${TMPDIR:-/tmp}/cluverse-local-map-canary"
mkdir -p "$LEDGER_DIR"
LEDGER="${LEDGER_DIR}/$(date +%F).count"
LOCK_DIR="${LEDGER}.lock"
if ! mkdir "$LOCK_DIR" 2>/dev/null; then
  echo "중단: 다른 네이버 canary가 실행 중이거나 잠금이 남아 있습니다: ${LOCK_DIR}" >&2
  exit 2
fi
USED=0
[[ ! -f "$LEDGER" ]] || USED="$(tr -cd '0-9' < "$LEDGER")"
USED="${USED:-0}"
(( USED + CALLS <= 10 )) || { echo "중단: 오늘 canary 누적 10회 제한을 넘습니다." >&2; exit 2; }

BODY_FILE=""
cleanup() {
  if [[ -n "$BODY_FILE" && -f "$BODY_FILE" ]]; then
    rm -f -- "$BODY_FILE"
  fi
  rmdir "$LOCK_DIR" 2>/dev/null || true
}
trap cleanup EXIT

for (( index=1; index<=CALLS; index++ )); do
  BODY_FILE="$(mktemp)"
  STATUS="$(curl --silent --show-error --output "$BODY_FILE" --write-out '%{http_code}' --get \
    -H "Authorization: Bearer ${AUTH_TOKEN}" \
    --data-urlencode "query=${QUERY}" \
    "${BASE_URL}/api/v2/places/search")"
  USED=$(( USED + 1 ))
  printf '%s\n' "$USED" > "$LEDGER"
  if [[ "$STATUS" == "429" ]]; then
    echo "429 수신: 재시도 없이 즉시 중단합니다." >&2
    exit 3
  fi
  [[ "$STATUS" == "200" ]] || { echo "실패: HTTP ${STATUS}" >&2; exit 3; }
  jq -e '.data.places | type == "array"' "$BODY_FILE" >/dev/null
  jq -e '.data.places[] | (.name | length > 0) and (.selectionToken | length > 20)' "$BODY_FILE" >/dev/null
  rm -f -- "$BODY_FILE"
  BODY_FILE=""
  if (( index < CALLS )); then
    case "$QPS" in
      1) sleep 1 ;;
      2) sleep 0.5 ;;
      3) sleep 0.334 ;;
      4) sleep 0.25 ;;
      5) sleep 0.2 ;;
    esac
  fi
done

echo "통과: 실제 네이버 응답 계약 ${CALLS}회 확인 (오늘 누적 ${USED}/10)"
