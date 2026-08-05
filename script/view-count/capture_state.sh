#!/usr/bin/env bash
set -euo pipefail

REDIS_HOST="${REDIS_HOST:-127.0.0.1}"
REDIS_PORT="${REDIS_PORT:-6379}"
RESULT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/results/raw"
OUTPUT="${1:-$RESULT_DIR/$(date +%Y%m%d-%H%M%S)-redis-state.txt}"
mkdir -p "$(dirname "$OUTPUT")"
{
  date -Iseconds
  redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" INFO memory | grep -E '^(used_memory:|used_memory_rss:|mem_fragmentation_ratio:)'
  for pattern in 'view:v2:delta:*' 'view:v3:delta:*' 'view:v4:counter:*' 'view:v4:dedupe:*'; do
    count="$(redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" --scan --pattern "$pattern" | wc -l | tr -d ' ')"
    echo "$pattern=$count"
  done
} | tee "$OUTPUT"
