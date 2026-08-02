#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

MODE="${1:-}"
VERSION=""
case "$MODE" in
  read)
    VERSION="${2:-}"
    if [[ "$VERSION" != "v1" && "$VERSION" != "v2" ]]; then
      echo "사용법: run.sh read v1|v2 [k6 인자...]" >&2
      exit 1
    fi
    shift 2
    K6_SCRIPT="k6/comment-page.k6.js"
    ;;
  correctness)
    shift
    K6_SCRIPT="k6/comment-correctness.k6.js"
    ;;
  write-root|write-reply)
    shift
    K6_SCRIPT="k6/comment-write.k6.js"
    ;;
  *)
    echo "사용법: run.sh read v1|v2 [k6 인자...]" >&2
    echo "       run.sh correctness|write-root|write-reply [k6 인자...]" >&2
    exit 1
    ;;
esac

: "${POST_ID:?POST_ID가 필요합니다.}"
BASE_URL="${BASE_URL:-http://localhost:8080}"
COMMENT_COUNT="${COMMENT_COUNT:-unknown}"
TREE_SHAPE="${TREE_SHAPE:-mixed}"
STAMP="$(date +%F-%H%M%S)"
mkdir -p results/raw

if [[ "$MODE" == "read" ]]; then
  NAME="${STAMP}-${MODE}-${VERSION}-comments${COMMENT_COUNT}-${TREE_SHAPE}"
elif [[ "$MODE" == "write-root" || "$MODE" == "write-reply" ]]; then
  : "${AUTH_TOKEN:?쓰기 측정에는 AUTH_TOKEN이 필요합니다.}"
  VERSION="v2"
  NAME="${STAMP}-${MODE}-${VERSION}"
else
  NAME="${STAMP}-${MODE}-comments${COMMENT_COUNT}-${TREE_SHAPE}"
fi

SUMMARY="results/raw/${NAME}-summary.json"
TIMESERIES="results/raw/${NAME}-timeseries.csv"
REPORT="results/raw/${NAME}.html"
CONSOLE="results/raw/${NAME}-console.txt"
RUN_ID="${RUN_ID:-$NAME}"
export BASE_URL POST_ID VERSION COMMENT_COUNT TREE_SHAPE AUTH_TOKEN RUN_ID
export K6_WEB_DASHBOARD="${K6_WEB_DASHBOARD:-true}"
export K6_WEB_DASHBOARD_EXPORT="${K6_WEB_DASHBOARD_EXPORT:-$REPORT}"
REPORT="$K6_WEB_DASHBOARD_EXPORT"

if [[ "$MODE" == "write-root" ]]; then
  export WRITE_KIND="root"
elif [[ "$MODE" == "write-reply" ]]; then
  : "${PARENT_COMMENT_ID:?write-reply에는 PARENT_COMMENT_ID가 필요합니다.}"
  export WRITE_KIND="reply"
fi

K6_ENV=(
  -e "BASE_URL=${BASE_URL}"
  -e "POST_ID=${POST_ID}"
  -e "COMMENT_COUNT=${COMMENT_COUNT}"
  -e "TREE_SHAPE=${TREE_SHAPE}"
)
for name in VERSION RATE DURATION LIMIT CURSOR_STEPS PRE_ALLOCATED_VUS MAX_VUS WRITE_KIND PARENT_COMMENT_ID MAX_PAGES RUN_ID; do
  if [[ -n "${!name:-}" ]]; then
    K6_ENV+=(-e "${name}=${!name}")
  fi
done

{
  echo "[comment-pagination] mode=${MODE} version=${VERSION:-comparison}"
  echo "[condition] base_url=${BASE_URL} post_id=${POST_ID} comments=${COMMENT_COUNT} tree_shape=${TREE_SHAPE}"
  echo "[condition] rate=${RATE:-script-default} duration=${DURATION:-script-default} limit=${LIMIT:-script-default} cursor_steps=${CURSOR_STEPS:-0} run_id=${RUN_ID}"
  k6 run \
    --summary-export "$SUMMARY" \
    --out "csv=${TIMESERIES}" \
    "${K6_ENV[@]}" \
    "$@" \
    "$K6_SCRIPT"
} 2>&1 | tee "$CONSOLE"

echo "capture HTML: ${REPORT}"
echo "console text: script/comment-pagination/${CONSOLE}"
echo "summary JSON: script/comment-pagination/${SUMMARY}"
echo "timeseries CSV: script/comment-pagination/${TIMESERIES}"
