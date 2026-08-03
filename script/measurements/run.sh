#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/../.."

PYTHON_BIN="${PYTHON_BIN:-python3}"
OUTPUT_DIR="${OUTPUT_DIR:-script/measurements/results/$(date +%F-%H%M%S)}"

"$PYTHON_BIN" script/measurements/plot_results.py \
  --input script/popularity/results/raw \
  --input script/view-surge/results/raw \
  --input script/local-map/results/raw \
  --input script/comment-pagination/results/raw \
  --input script/home-feed/results/raw \
  --output-dir "$OUTPUT_DIR" \
  "$@"
