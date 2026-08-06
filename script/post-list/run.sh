#!/usr/bin/env bash
# k6 실행 래퍼 — 웹 대시보드 실시간 보기(localhost:5665) + HTML 리포트 저장을 기본으로 켠다.
# 리포트는 results/raw/<날짜시각>-<bench|cursor|realistic>[-vN].html 로 자동 저장된다.
#
# 사용법: k6 인자를 그대로 뒤에 붙인다.
#   script/post-list/run.sh bench  -e VERSION=v1 -e RATE=100 -e DURATION=2m
#   script/post-list/run.sh cursor -e RATE=100 -e DURATION=2m
#   script/post-list/run.sh realistic -e RATE=200 -e V4_REQUEST_SHARE=0.05 -e DURATION=10m
#
# VU 풀 기본값(PRE_ALLOCATED_VUS=100, MAX_VUS=600)도 여기서 깔아준다.
# 100 rps × 원격 지연에서 기본 VU 풀(50)이 바닥나 dropped_iterations 로 측정이
# 무효가 되는 사고 방지용 — 필요하면 환경변수로 오버라이드.
set -euo pipefail
cd "$(dirname "$0")"

KIND="${1:-}"
case "$KIND" in
  bench)     SCRIPT=k6/post-list-bench.k6.js ;;
  cursor)    SCRIPT=k6/post-list-cursor.k6.js ;;
  realistic) SCRIPT=k6/post-list-realistic.k6.js ;;
  *) echo "사용법: run.sh bench|cursor|realistic [k6 인자...]" >&2; exit 1 ;;
esac
shift

# -e VERSION=vN 이 있으면 리포트 파일명에 붙인다
LABEL="$KIND"
[ "$KIND" = "realistic" ] && LABEL="realistic-v3-v4"
for arg in "$@"; do
  case "$arg" in
    VERSION=*) LABEL="$LABEL-${arg#VERSION=}" ;;
    CACHE_MODE=*) LABEL="$LABEL-cache-${arg#CACHE_MODE=}" ;;
  esac
done

mkdir -p results/raw
REPORT="results/raw/$(date +%F-%H%M%S)-$LABEL.html"

export K6_WEB_DASHBOARD=true
export K6_WEB_DASHBOARD_EXPORT="$REPORT"
export PRE_ALLOCATED_VUS="${PRE_ALLOCATED_VUS:-100}"
export MAX_VUS="${MAX_VUS:-600}"

k6 run "$@" "$SCRIPT"
echo
echo "HTML 리포트 저장됨: script/post-list/$REPORT"
