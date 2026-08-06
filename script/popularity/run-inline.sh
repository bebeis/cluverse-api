#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
SCRIPT_DIR="$ROOT_DIR/script/popularity"
RESULT_DIR="$SCRIPT_DIR/results/raw"

: "${CONDITION:?CONDITION=disabled|enabled 필요}"
: "${KIND:?KIND=like|comment 필요}"
: "${REPEAT:?REPEAT=1|2|3 필요}"

case "$CONDITION" in disabled|enabled) ;; *) echo "CONDITION은 disabled|enabled" >&2; exit 1 ;; esac
case "$KIND" in like|comment) ;; *) echo "KIND는 like|comment" >&2; exit 1 ;; esac
case "$REPEAT" in 1|2|3) ;; *) echo "REPEAT는 1|2|3" >&2; exit 1 ;; esac

mkdir -p "$RESULT_DIR"
STAMP="$(date +%Y%m%d-%H%M%S)"
NAME="${STAMP}-inline-${CONDITION}-${KIND}-r${REPEAT}"
SUMMARY="$RESULT_DIR/${NAME}-summary.json"
HTML="$RESULT_DIR/${NAME}.html"
# constant-arrival-rate는 60초 경계에서 301번째 iteration을 만들 수 있다.
# 반복 구간 사이에 여유를 둬 좋아요 중복과 댓글 snapshot 오염을 막는다.
POST_OFFSET=$(( (REPEAT - 1) * 320 ))

export CONDITION KIND POST_OFFSET
K6_WEB_DASHBOARD=true K6_WEB_DASHBOARD_EXPORT="$HTML" \
  k6 run --quiet --summary-export "$SUMMARY" \
  "$SCRIPT_DIR/k6/popularity-inline-overhead.k6.js"
python3 "$SCRIPT_DIR/collect_inline_results.py" "$SUMMARY"

echo "summary: $SUMMARY"
echo "dashboard: $HTML"
