#!/usr/bin/env bash
# k6 실행 래퍼 — 웹 대시보드 실시간 보기(localhost:5665) + HTML/summary JSON 저장을 기본으로 켠다.
# HTML은 <라벨>.html, JSON은 <라벨>-summary.json 형태로 results/raw에 저장된다.
#
# 사용법: k6 인자를 그대로 뒤에 붙인다.
#   script/view-surge/run.sh bench     -e VERSION=v3 -e RATE=300 -e DURATION=1m
#   script/view-surge/run.sh bench     -e VERSION=v3 -e EXECUTOR=steps -e STEP_RATES=50,100,150,200,250,300
#   script/view-surge/run.sh lifecycle -e HOT_POST_ID=5999999
#
# VU 풀 기본값(PRE_ALLOCATED_VUS=100, MAX_VUS=600)도 여기서 깔아준다.
# 급상승 시나리오는 램프 구간에서 도착률이 계속 오르므로, 기본 VU 풀이 바닥나
# dropped_iterations 로 측정이 무효가 되는 사고를 막는다 — 필요하면 환경변수로 오버라이드.
set -euo pipefail
cd "$(dirname "$0")"

KIND="${1:-}"
case "$KIND" in
  bench)     SCRIPT=k6/view-surge-bench.k6.js ;;
  lifecycle) SCRIPT=k6/view-surge-lifecycle.k6.js ;;
  *) echo "사용법: run.sh bench|lifecycle [k6 인자...]" >&2; exit 1 ;;
esac
shift

# -e VERSION=vN 이 있으면 리포트 파일명에 붙인다
LABEL="$KIND"
for arg in "$@"; do
  case "$arg" in VERSION=*) LABEL="$KIND-${arg#VERSION=}" ;; esac
done

mkdir -p results/raw
STAMP="$(date +%F-%H%M%S)"
REPORT="results/raw/$STAMP-$LABEL.html"
SUMMARY="results/raw/$STAMP-$LABEL-summary.json"

export K6_WEB_DASHBOARD=true
export K6_WEB_DASHBOARD_EXPORT="$REPORT"
export PRE_ALLOCATED_VUS="${PRE_ALLOCATED_VUS:-100}"
export MAX_VUS="${MAX_VUS:-600}"

k6 run --summary-export "$SUMMARY" "$@" "$SCRIPT"
echo
echo "HTML 리포트 저장됨: script/view-surge/$REPORT"
echo "summary JSON 저장됨: script/view-surge/$SUMMARY"
