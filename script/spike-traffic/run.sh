#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
cd "$ROOT_DIR"

PROFILE="${1:-}"
case "$PROFILE" in
  capacity|spike|smoke) shift ;;
  *)
    echo "사용법: script/spike-traffic/run.sh capacity|spike|smoke [k6 인자...]" >&2
    exit 1
    ;;
esac

command -v k6 >/dev/null || { echo "k6가 필요합니다." >&2; exit 1; }
command -v jq >/dev/null || { echo "jq가 필요합니다." >&2; exit 1; }

BASE_URL="${BASE_URL:-http://localhost:8080}"
MEMBER_IDS="${MEMBER_IDS:-1}"
BOARD_IDS="${BOARD_IDS:-1}"
POST_IDS="${POST_IDS:-1}"
LABEL="${LABEL:-baseline}"
SAFE_LABEL="$(printf '%s' "$LABEL" | tr -cs '[:alnum:]._- ' '-' | tr ' ' '-' | sed 's/^-*//;s/-*$//')"
STAMP="$(date +%F-%H%M%S)"
RUN_ID="${RUN_ID:-${STAMP}-${PROFILE}-${SAFE_LABEL:-run}}"
RUN_DIR="$SCRIPT_DIR/results/raw/$RUN_ID"
mkdir -p "$RUN_DIR"

case "$BASE_URL" in
  http://localhost|http://localhost:*|http://127.0.0.1|http://127.0.0.1:*|http://\[::1\]|http://\[::1\]:*) ;;
  *)
    if [[ "${CONFIRM_LOAD_TEST:-}" != "1" ]]; then
      echo "원격 대상($BASE_URL)에는 CONFIRM_LOAD_TEST=1이 필요합니다." >&2
      exit 1
    fi
    ;;
esac

WEIGHT_HOME_RECENT="${WEIGHT_HOME_RECENT:-15}"
WEIGHT_POPULAR_POSTS="${WEIGHT_POPULAR_POSTS:-10}"
WEIGHT_POST_LIST="${WEIGHT_POST_LIST:-25}"
WEIGHT_POST_DETAIL="${WEIGHT_POST_DETAIL:-20}"
WEIGHT_COMMENT_LIST="${WEIGHT_COMMENT_LIST:-15}"
WEIGHT_VIEW_COUNT="${WEIGHT_VIEW_COUNT:-10}"
WEIGHT_COMMENT_WRITE="${WEIGHT_COMMENT_WRITE:-5}"

if [[ "$WEIGHT_POST_DETAIL" != "0" || "$WEIGHT_VIEW_COUNT" != "0" || "$WEIGHT_COMMENT_WRITE" != "0" ]]; then
  if [[ "${ALLOW_DATA_MUTATION:-}" != "1" ]]; then
    echo "게시글 상세는 조회수를 변경하며 쓰기 flow도 포함됩니다." >&2
    echo "test fixture임을 확인한 뒤 ALLOW_DATA_MUTATION=1을 지정하세요." >&2
    exit 1
  fi
fi

CAPACITY_RATES="${CAPACITY_RATES:-25,50,100,150,200}"
STEP_DURATION_SECONDS="${STEP_DURATION_SECONDS:-600}"
STEP_SETTLE_SECONDS="${STEP_SETTLE_SECONDS:-30}"
NORMAL_RATE="${NORMAL_RATE:-50}"
SPIKE_RATE="${SPIKE_RATE:-250}"
BASELINE_SECONDS="${BASELINE_SECONDS:-120}"
RAMP_SECONDS="${RAMP_SECONDS:-10}"
SPIKE_SECONDS="${SPIKE_SECONDS:-120}"
RECOVERY_SECONDS="${RECOVERY_SECONDS:-120}"
NORMALIZATION_WINDOW_SECONDS="${NORMALIZATION_WINDOW_SECONDS:-30}"
SMOKE_RATE="${SMOKE_RATE:-1}"
SMOKE_SECONDS="${SMOKE_SECONDS:-10}"
READ_P95_MS="${READ_P95_MS:-300}"
READ_P99_MS="${READ_P99_MS:-800}"
WRITE_P95_MS="${WRITE_P95_MS:-500}"
WRITE_P99_MS="${WRITE_P99_MS:-1500}"
SUCCESS_RATE="${SUCCESS_RATE:-0.999}"
HOT_POST_COUNT="${HOT_POST_COUNT:-10}"
HOT_POST_SHARE="${HOT_POST_SHARE:-0.8}"
PRE_ALLOCATED_VUS="${PRE_ALLOCATED_VUS:-}"
MAX_VUS="${MAX_VUS:-}"

START_EPOCH="$(date +%s)"
STARTED_AT="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
GIT_REVISION="$(git rev-parse --short HEAD 2>/dev/null || printf 'unknown')"

jq -n \
  --arg run_id "$RUN_ID" \
  --arg label "$LABEL" \
  --arg profile "$PROFILE" \
  --arg started_at "$STARTED_AT" \
  --arg base_url "$BASE_URL" \
  --arg git_revision "$GIT_REVISION" \
  --arg dataset "${DATASET_LABEL:-unspecified}" \
  --arg cache_condition "${CACHE_CONDITION:-unspecified}" \
  --arg app_spec "${APP_SPEC:-unspecified}" \
  --arg db_spec "${DB_SPEC:-unspecified}" \
  --arg redis_spec "${REDIS_SPEC:-unspecified}" \
  --arg load_generator_spec "${LOAD_GENERATOR_SPEC:-unspecified}" \
  --arg capacity_rates "$CAPACITY_RATES" \
  --argjson start_epoch "$START_EPOCH" \
  --argjson step_duration "$STEP_DURATION_SECONDS" \
  --argjson settle_seconds "$STEP_SETTLE_SECONDS" \
  --argjson normal_rate "$NORMAL_RATE" \
  --argjson spike_rate "$SPIKE_RATE" \
  --argjson baseline_seconds "$BASELINE_SECONDS" \
  --argjson ramp_seconds "$RAMP_SECONDS" \
  --argjson spike_seconds "$SPIKE_SECONDS" \
  --argjson recovery_seconds "$RECOVERY_SECONDS" \
  --argjson normalization_window "$NORMALIZATION_WINDOW_SECONDS" \
  --argjson smoke_rate "$SMOKE_RATE" \
  --argjson smoke_seconds "$SMOKE_SECONDS" \
  --argjson read_p95 "$READ_P95_MS" \
  --argjson read_p99 "$READ_P99_MS" \
  --argjson write_p95 "$WRITE_P95_MS" \
  --argjson write_p99 "$WRITE_P99_MS" \
  --argjson success_rate "$SUCCESS_RATE" \
  --argjson app_instances "${APP_INSTANCES:-1}" \
  --argjson hot_post_count "$HOT_POST_COUNT" \
  --argjson hot_post_share "$HOT_POST_SHARE" \
  --argjson weight_home "$WEIGHT_HOME_RECENT" \
  --argjson weight_popular "$WEIGHT_POPULAR_POSTS" \
  --argjson weight_list "$WEIGHT_POST_LIST" \
  --argjson weight_detail "$WEIGHT_POST_DETAIL" \
  --argjson weight_comments "$WEIGHT_COMMENT_LIST" \
  --argjson weight_views "$WEIGHT_VIEW_COUNT" \
  --argjson weight_comment_write "$WEIGHT_COMMENT_WRITE" \
  '{
    run_id: $run_id,
    label: $label,
    profile: $profile,
    started_at: $started_at,
    start_epoch: $start_epoch,
    base_url: $base_url,
    git_revision: $git_revision,
    environment: {
      dataset: $dataset,
      cache_condition: $cache_condition,
      app_spec: $app_spec,
      app_instances: $app_instances,
      db_spec: $db_spec,
      redis_spec: $redis_spec,
      load_generator_spec: $load_generator_spec
    },
    workload: {
      hot_post_count: $hot_post_count,
      hot_post_share: $hot_post_share,
      weights: {
        home_recent: $weight_home,
        popular_posts: $weight_popular,
        post_list: $weight_list,
        post_detail: $weight_detail,
        comment_list: $weight_comments,
        view_count: $weight_views,
        comment_write: $weight_comment_write
      }
    },
    load: {
      capacity_rates: ($capacity_rates | split(",") | map(tonumber)),
      step_duration_seconds: $step_duration,
      settle_seconds: $settle_seconds,
      normal_rate: $normal_rate,
      spike_rate: $spike_rate,
      baseline_seconds: $baseline_seconds,
      ramp_seconds: $ramp_seconds,
      spike_seconds: $spike_seconds,
      recovery_seconds: $recovery_seconds,
      normalization_window_seconds: $normalization_window,
      smoke_rate: $smoke_rate,
      smoke_seconds: $smoke_seconds
    },
    slo: {
      read_p95_ms: $read_p95,
      read_p99_ms: $read_p99,
      write_p95_ms: $write_p95,
      write_p99_ms: $write_p99,
      success_rate: $success_rate
    }
  }' > "$RUN_DIR/metadata.json"

export PROFILE BASE_URL MEMBER_IDS BOARD_IDS POST_IDS LABEL RUN_ID
export CONFIRM_LOAD_TEST="${CONFIRM_LOAD_TEST:-}" ALLOW_DATA_MUTATION="${ALLOW_DATA_MUTATION:-}"
export CAPACITY_RATES STEP_DURATION_SECONDS NORMAL_RATE SPIKE_RATE BASELINE_SECONDS RAMP_SECONDS
export SPIKE_SECONDS RECOVERY_SECONDS SMOKE_RATE SMOKE_SECONDS READ_P95_MS READ_P99_MS WRITE_P95_MS
export WRITE_P99_MS SUCCESS_RATE HOT_POST_COUNT HOT_POST_SHARE PRE_ALLOCATED_VUS MAX_VUS
export WEIGHT_HOME_RECENT WEIGHT_POPULAR_POSTS WEIGHT_POST_LIST WEIGHT_POST_DETAIL
export WEIGHT_COMMENT_LIST WEIGHT_VIEW_COUNT WEIGHT_COMMENT_WRITE
export K6_WEB_DASHBOARD="${K6_WEB_DASHBOARD:-true}"
export K6_WEB_DASHBOARD_EXPORT="$RUN_DIR/k6-report.html"

echo "[spike-traffic] run_id=$RUN_ID profile=$PROFILE label=$LABEL"
echo "[target] $BASE_URL"
echo "[evidence] $RUN_DIR"

set +e
k6 run \
  --summary-export "$RUN_DIR/k6-summary.json" \
  --out "csv=$RUN_DIR/k6-timeseries-raw.csv" \
  "$@" \
  "$SCRIPT_DIR/k6/core-workload.k6.js" \
  2>&1 | tee "$RUN_DIR/k6-console.txt"
K6_STATUS="${PIPESTATUS[0]}"
set -e

END_EPOCH="$(date +%s)"
ENDED_AT="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
METADATA_TMP="$(mktemp "$RUN_DIR/metadata.XXXXXX")"
jq \
  --arg ended_at "$ENDED_AT" \
  --argjson end_epoch "$END_EPOCH" \
  --argjson k6_exit_code "$K6_STATUS" \
  '. + {ended_at: $ended_at, end_epoch: $end_epoch, k6_exit_code: $k6_exit_code}' \
  "$RUN_DIR/metadata.json" > "$METADATA_TMP"
mv "$METADATA_TMP" "$RUN_DIR/metadata.json"

if [[ -n "${PROMETHEUS_URL:-}" ]]; then
  PROMETHEUS_SETTLE_SECONDS="${PROMETHEUS_SETTLE_SECONDS:-15}"
  if [[ "$PROMETHEUS_SETTLE_SECONDS" -gt 0 ]]; then
    echo "[prometheus] 마지막 scrape 대기 ${PROMETHEUS_SETTLE_SECONDS}s"
    sleep "$PROMETHEUS_SETTLE_SECONDS"
  fi
  python3 "$SCRIPT_DIR/collect_prometheus.py" \
    --url "$PROMETHEUS_URL" \
    --start "$((START_EPOCH - 30))" \
    --end "$((END_EPOCH + PROMETHEUS_SETTLE_SECONDS))" \
    --origin "$START_EPOCH" \
    --step "${PROMETHEUS_STEP_SECONDS:-15}" \
    --output-dir "$RUN_DIR" || true
fi

ANALYZE_ARGS=(--run-dir "$RUN_DIR")
if ! python3 -c 'import matplotlib' >/dev/null 2>&1; then
  echo "[plot] matplotlib 미설치 — CSV와 HTML 표만 생성합니다." >&2
  echo "       python3 -m pip install -r script/spike-traffic/requirements.txt" >&2
  ANALYZE_ARGS+=(--skip-charts)
fi
python3 "$SCRIPT_DIR/analyze.py" "${ANALYZE_ARGS[@]}"

echo
if [[ -f "$RUN_DIR/k6-report.html" ]]; then
  echo "k6 HTML      : $RUN_DIR/k6-report.html"
else
  echo "k6 HTML      : 생성되지 않음(K6_WEB_DASHBOARD 설정 또는 실행 길이 확인)"
fi
echo "capture HTML : $RUN_DIR/report.html"
echo "normalized   : $RUN_DIR/k6-timeseries.csv"
echo "SLO steps    : $RUN_DIR/slo-steps.csv"
if [[ "$K6_STATUS" -ne 0 ]]; then
  echo "k6 threshold 실패(exit=$K6_STATUS). 증거 수집과 그래프 생성은 완료했습니다." >&2
fi
exit "$K6_STATUS"
