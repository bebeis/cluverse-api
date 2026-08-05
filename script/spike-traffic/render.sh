#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
RUN_DIR="${1:-}"
if [[ -z "$RUN_DIR" ]]; then
  echo "사용법: script/spike-traffic/render.sh script/spike-traffic/results/raw/<run-id>" >&2
  exit 1
fi

python3 -c 'import matplotlib' >/dev/null 2>&1 || {
  echo "matplotlib이 필요합니다: python3 -m pip install -r script/spike-traffic/requirements.txt" >&2
  exit 1
}
python3 "$SCRIPT_DIR/analyze.py" --run-dir "$RUN_DIR"
