#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
SCRIPT="$ROOT_DIR/script/post-list/k6/post-list-realistic.k6.js"
RUNNER="$ROOT_DIR/script/post-list/run.sh"

plan="$(k6 inspect -e RATE=200 -e V4_REQUEST_SHARE=0.05 "$SCRIPT")"
PLAN="$plan" python3 - <<'PY'
import json
import os

plan = json.loads(os.environ["PLAN"])
scenarios = plan["scenarios"]
assert scenarios["v3_offset_requests"]["rate"] == 190
assert scenarios["v4_cursor_requests"]["rate"] == 10
assert scenarios["v3_offset_requests"]["exec"] == "v3Request"
assert scenarios["v4_cursor_requests"]["exec"] == "v4Request"
PY

if k6 inspect -e VERSION=v4 "$SCRIPT" >/dev/null 2>&1; then
  echo "realistic 시나리오는 VERSION 단독 지정을 거부해야 합니다." >&2
  exit 1
fi

if k6 inspect -e V4_REQUEST_SHARE=1 "$SCRIPT" >/dev/null 2>&1; then
  echo "V4_REQUEST_SHARE=1은 혼합 시나리오가 아니므로 거부해야 합니다." >&2
  exit 1
fi

"$RUNNER" realistic --help >/dev/null

echo "realistic plan tests passed"
