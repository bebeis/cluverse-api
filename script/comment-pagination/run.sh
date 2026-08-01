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
    echo "사용법: run.sh read v1|v2|correctness|write-root|write-reply [k6 인자...]" >&2
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
export BASE_URL POST_ID VERSION COMMENT_COUNT TREE_SHAPE

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
for name in VERSION AUTH_TOKEN RATE DURATION LIMIT CURSOR_STEPS PRE_ALLOCATED_VUS MAX_VUS WRITE_KIND PARENT_COMMENT_ID MAX_PAGES; do
  if [[ -n "${!name:-}" ]]; then
    K6_ENV+=(-e "${name}=${!name}")
  fi
done

k6 run \
  --summary-export "$SUMMARY" \
  --out "csv=${TIMESERIES}" \
  "${K6_ENV[@]}" \
  "$@" \
  "$K6_SCRIPT"

echo "summary JSON: script/comment-pagination/${SUMMARY}"
echo "timeseries CSV: script/comment-pagination/${TIMESERIES}"
