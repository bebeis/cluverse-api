#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SQL_FILE="${1:-}"
LABEL="${2:-query}"
if [[ -z "$SQL_FILE" || ! -f "$SQL_FILE" ]]; then
  echo "사용법: script/spike-traffic/explain/capture.sh <explain.sql> [label]" >&2
  exit 1
fi

if rg -i '\b(insert|update|delete|replace|drop|alter|truncate|create|grant|revoke)\b' "$SQL_FILE" >/dev/null; then
  echo "안전하지 않은 SQL 키워드가 포함되어 실행하지 않습니다: $SQL_FILE" >&2
  exit 1
fi
if ! rg -i 'EXPLAIN[[:space:]]+ANALYZE' "$SQL_FILE" >/dev/null; then
  echo "SQL 파일에 EXPLAIN ANALYZE가 필요합니다." >&2
  exit 1
fi

command -v mysql >/dev/null || { echo "mysql client가 필요합니다." >&2; exit 1; }
SAFE_LABEL="$(printf '%s' "$LABEL" | tr -cs '[:alnum:]._- ' '-' | tr ' ' '-' | sed 's/^-*//;s/-*$//')"
STAMP="$(date +%F-%H%M%S)"
OUTPUT_DIR="${OUTPUT_DIR:-$SCRIPT_DIR/../results/raw/${STAMP}-explain-${SAFE_LABEL:-query}}"
mkdir -p "$OUTPUT_DIR"
RAW_PLAN="$OUTPUT_DIR/explain-plan.txt"

MYSQL_ARGS=(
  --host "${MYSQL_HOST:-127.0.0.1}"
  --port "${MYSQL_PORT:-3306}"
  --user "${MYSQL_USER:-cluverse}"
  --database "${MYSQL_DATABASE:-cluverse}"
  --raw
  --batch
  --skip-column-names
  --connect-timeout "${MYSQL_CONNECT_TIMEOUT:-5}"
)

echo "[explain] label=$LABEL sql=$SQL_FILE"
echo "[mysql] ${MYSQL_HOST:-127.0.0.1}:${MYSQL_PORT:-3306}/${MYSQL_DATABASE:-cluverse}"
mysql "${MYSQL_ARGS[@]}" < "$SQL_FILE" | tee "$RAW_PLAN"
python3 "$SCRIPT_DIR/summarize_explain.py" \
  --input "$RAW_PLAN" \
  --label "$LABEL" \
  --output-dir "$OUTPUT_DIR"

echo "capture HTML : $OUTPUT_DIR/explain-report.html"
echo "plan nodes    : $OUTPUT_DIR/explain-nodes.csv"
echo "raw plan      : $RAW_PLAN"
