#!/usr/bin/env bash
# k6 실행 래퍼. HTML 대시보드와 기계 판독 가능한 summary JSON을 함께 남긴다.
set -euo pipefail

cd "$(dirname "$0")"

KIND="${1:-}"
case "$KIND" in
  bench) SCRIPT="k6/popularity-bench.k6.js" ;;
  lifecycle) SCRIPT="k6/popularity-lifecycle.k6.js" ;;
  recall) SCRIPT="k6/popularity-recall.k6.js" ;;
  *)
    echo "사용법: run.sh bench|lifecycle|recall [k6 인자...]" >&2
    exit 1
    ;;
esac
shift

LABEL="$KIND"
EXPECT_VERSION_VALUE=false
for arg in "$@"; do
  if [[ "$EXPECT_VERSION_VALUE" == true ]]; then
    case "$arg" in
      VERSION=*) LABEL="$KIND-${arg#VERSION=}" ;;
    esac
    EXPECT_VERSION_VALUE=false
    continue
  fi
  case "$arg" in
    VERSION=*) LABEL="$KIND-${arg#VERSION=}" ;;
    -e|--env) EXPECT_VERSION_VALUE=true ;;
  esac
done

mkdir -p results/raw
STAMP="$(date +%F-%H%M%S)"
REPORT="results/raw/$STAMP-$LABEL.html"
SUMMARY="results/raw/$STAMP-$LABEL-summary.json"

export K6_WEB_DASHBOARD="${K6_WEB_DASHBOARD:-true}"
export K6_WEB_DASHBOARD_EXPORT="${K6_WEB_DASHBOARD_EXPORT:-$REPORT}"
export PRE_ALLOCATED_VUS="${PRE_ALLOCATED_VUS:-100}"
export MAX_VUS="${MAX_VUS:-600}"

k6 run --summary-export "$SUMMARY" "$@" "$SCRIPT"

echo
echo "HTML 리포트: script/popularity/$REPORT"
echo "summary JSON: script/popularity/$SUMMARY"
