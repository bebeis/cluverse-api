#!/usr/bin/env bash
set -euo pipefail

REDIS_HOST="${REDIS_HOST:-127.0.0.1}"
REDIS_PORT="${REDIS_PORT:-6379}"
BATCH_SIZE="${BATCH_SIZE:-500}"
PATTERNS=(
  'view:v4:counter:*'
  'view:v4:dedupe:*'
  'view:v4:init:*'
)

if [ "${1:-}" = "--dry-run" ]; then
  printf '%s\n' "${PATTERNS[@]}"
  exit 0
fi
[ "$#" -eq 0 ] || { echo "usage: $0 [--dry-run]" >&2; exit 2; }

if [ -n "${REDIS_CLI:-}" ]; then
  REDIS_COMMAND=("$REDIS_CLI")
elif command -v redis-cli >/dev/null 2>&1; then
  REDIS_COMMAND=(redis-cli)
elif command -v redis6-cli >/dev/null 2>&1; then
  REDIS_COMMAND=(redis6-cli)
else
  echo "redis-cli 또는 redis6-cli가 필요합니다." >&2
  exit 1
fi
REDIS_COMMAND+=(-h "$REDIS_HOST" -p "$REDIS_PORT" --raw)

"${REDIS_COMMAND[@]}" PING >/dev/null

DELETED=0
KEY_BATCH=()
delete_batch() {
  [ "${#KEY_BATCH[@]}" -eq 0 ] && return
  local removed
  removed="$("${REDIS_COMMAND[@]}" UNLINK "${KEY_BATCH[@]}")"
  DELETED=$((DELETED + removed))
  KEY_BATCH=()
}

for pattern in "${PATTERNS[@]}"; do
  while IFS= read -r key; do
    [ -z "$key" ] && continue
    KEY_BATCH+=("$key")
    if [ "${#KEY_BATCH[@]}" -ge "$BATCH_SIZE" ]; then
      delete_batch
    fi
  done < <("${REDIS_COMMAND[@]}" --scan --pattern "$pattern")
  delete_batch
done

echo "조회수 Redis 키 ${DELETED}개 삭제"
