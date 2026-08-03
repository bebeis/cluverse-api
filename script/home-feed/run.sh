#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

MODE="${1:-}"
VERSION=""
case "$MODE" in
  read)
    VERSION="${2:-}"
    if [[ "$VERSION" != "v1" && "$VERSION" != "v2" && "$VERSION" != "v3" ]]; then
      echo "사용법: run.sh read v1|v2|v3 [k6 인자...]" >&2
      exit 1
    fi
    shift 2
    K6_SCRIPT="k6/recent-commented-posts.k6.js"
    ;;
  correctness)
    shift
    K6_SCRIPT="k6/recent-commented-posts-correctness.k6.js"
    ;;
  write)
    shift
    : "${POST_ID:?write에는 POST_ID가 필요합니다.}"
    VERSION="v3"
    K6_SCRIPT="k6/comment-write.k6.js"
    ;;
  *)
    echo "사용법: run.sh read v1|v2|v3 [k6 인자...]" >&2
    echo "       run.sh correctness|write [k6 인자...]" >&2
    exit 1
    ;;
esac

: "${SESSION_COOKIE:?SESSION_COOKIE이 필요합니다. 예: JSESSIONID=...}"
BASE_URL="${BASE_URL:-http://localhost:8080}"
COMMENTS="${COMMENTS:-unknown}"
COMMENTED_POSTS="${COMMENTED_POSTS:-unknown}"
HOT_COMMENT_PERCENT="${HOT_COMMENT_PERCENT:-unknown}"
STAMP="$(date +%F-%H%M%S)"
RUN_ID="${RUN_ID:-${STAMP}-${MODE}-${VERSION:-comparison}}"
mkdir -p results/raw

if [[ "$MODE" == "read" ]]; then
  NAME="${STAMP}-${MODE}-${VERSION}-comments${COMMENTS}-posts${COMMENTED_POSTS}"
else
  NAME="${STAMP}-${MODE}-${VERSION:-comparison}"
fi

SUMMARY="results/raw/${NAME}-summary.json"
TIMESERIES="results/raw/${NAME}-timeseries.csv"
REPORT="results/raw/${NAME}.html"
CONSOLE="results/raw/${NAME}-console.txt"
export BASE_URL SESSION_COOKIE VERSION COMMENTS COMMENTED_POSTS HOT_COMMENT_PERCENT POST_ID RUN_ID
export K6_WEB_DASHBOARD="${K6_WEB_DASHBOARD:-true}"
export K6_WEB_DASHBOARD_EXPORT="${K6_WEB_DASHBOARD_EXPORT:-$REPORT}"
REPORT="$K6_WEB_DASHBOARD_EXPORT"

K6_ENV=()
for name in BASE_URL VERSION COMMENTS COMMENTED_POSTS HOT_COMMENT_PERCENT POST_ID RUN_ID RATE DURATION PRE_ALLOCATED_VUS MAX_VUS; do
  if [[ -n "${!name:-}" ]]; then
    K6_ENV+=(-e "${name}=${!name}")
  fi
done

{
  echo "[home-feed] mode=${MODE} version=${VERSION:-comparison}"
  echo "[condition] base_url=${BASE_URL} comments=${COMMENTS} commented_posts=${COMMENTED_POSTS} hot_comment_percent=${HOT_COMMENT_PERCENT}"
  echo "[condition] rate=${RATE:-script-default} duration=${DURATION:-script-default} post_id=${POST_ID:-none} run_id=${RUN_ID}"
  k6 run \
    --summary-export "$SUMMARY" \
    --out "csv=${TIMESERIES}" \
    "${K6_ENV[@]}" \
    "$@" \
    "$K6_SCRIPT"
} 2>&1 | tee "$CONSOLE"

echo "capture HTML: ${REPORT}"
echo "console text: script/home-feed/${CONSOLE}"
echo "summary JSON: script/home-feed/${SUMMARY}"
echo "timeseries CSV: script/home-feed/${TIMESERIES}"
