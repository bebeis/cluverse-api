#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
SCRIPT_DIR="$ROOT_DIR/script/view-count"
RESULT_DIR="$SCRIPT_DIR/results/raw"
MODE="${1:-bench}"
shift || true
K6_ARGS=("$@")
mkdir -p "$RESULT_DIR"
STAMP="$(date +%Y%m%d-%H%M%S)"
VERSION_LABEL="${VERSION:-unknown}"
POST_MODE_LABEL="${POST_MODE:-hot}"
for ((index = 0; index < ${#K6_ARGS[@]}; index++)); do
  if [[ "${K6_ARGS[$index]}" == "-e" && "${K6_ARGS[$((index + 1))]:-}" == VERSION=* ]]; then
    VERSION_LABEL="${K6_ARGS[$((index + 1))]#VERSION=}"
  fi
  if [[ "${K6_ARGS[$index]}" == "-e" && "${K6_ARGS[$((index + 1))]:-}" == POST_MODE=* ]]; then
    POST_MODE_LABEL="${K6_ARGS[$((index + 1))]#POST_MODE=}"
  fi
done
if [[ "$MODE" == "bench" ]]; then
  DEFAULT_LABEL="${MODE}-${VERSION_LABEL}-${POST_MODE_LABEL}"
else
  DEFAULT_LABEL="${MODE}-${VERSION_LABEL}"
fi
LABEL="${LABEL:-$DEFAULT_LABEL}"
SUMMARY="$RESULT_DIR/${STAMP}-${LABEL}-summary.json"
HTML="$RESULT_DIR/${STAMP}-${LABEL}.html"

case "$MODE" in
  bench) TEST_FILE="$SCRIPT_DIR/k6/view-count-bench.k6.js" ;;
  correctness) TEST_FILE="$SCRIPT_DIR/k6/view-count-correctness.k6.js" ;;
  regression) TEST_FILE="$SCRIPT_DIR/k6/view-count-regression.k6.js" ;;
  *) echo "usage: $0 bench|correctness|regression [k6 args...]" >&2; exit 2 ;;
esac

K6_WEB_DASHBOARD=true K6_WEB_DASHBOARD_EXPORT="$HTML" \
  k6 run --summary-export "$SUMMARY" "${K6_ARGS[@]}" "$TEST_FILE"
# regression은 metrics.csv 스키마(RPS/p99)와 지표가 달라 summary JSON만 남긴다.
[[ "$MODE" == "regression" ]] || python3 "$SCRIPT_DIR/collect_results.py" "$SUMMARY"
echo "summary: $SUMMARY"
echo "dashboard: $HTML"
