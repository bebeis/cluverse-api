#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
SCRIPT_DIR="$ROOT_DIR/script/popularity"
RESULT_DIR="$SCRIPT_DIR/results/raw"
K6_ARGS=("$@")
mkdir -p "$RESULT_DIR"

VERSION_LABEL="${VERSION:-unknown}"
for ((index = 0; index < ${#K6_ARGS[@]}; index++)); do
  if [[ "${K6_ARGS[$index]}" == "-e" && "${K6_ARGS[$((index + 1))]:-}" == VERSION=* ]]; then
    VERSION_LABEL="${K6_ARGS[$((index + 1))]#VERSION=}"
  fi
done

STAMP="$(date +%Y%m%d-%H%M%S)"
LABEL="${LABEL:-bench-${VERSION_LABEL}}"
SUMMARY="$RESULT_DIR/${STAMP}-${LABEL}-summary.json"
HTML="$RESULT_DIR/${STAMP}-${LABEL}.html"

K6_WEB_DASHBOARD=true K6_WEB_DASHBOARD_EXPORT="$HTML" \
  k6 run --summary-export "$SUMMARY" "${K6_ARGS[@]}" "$SCRIPT_DIR/k6/popularity-bench.k6.js"
python3 "$SCRIPT_DIR/collect_results.py" "$SUMMARY"
echo "summary: $SUMMARY"
echo "dashboard: $HTML"
