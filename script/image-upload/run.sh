#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BASE_URL="${BASE_URL:-http://localhost:8080}"
BENCHMARK_TOKEN="${BENCHMARK_TOKEN:?BENCHMARK_TOKEN is required}"
IMAGE_FILE="${IMAGE_FILE:?IMAGE_FILE is required}"
RESULT_DIR="${RESULT_DIR:-${SCRIPT_DIR}/results/$(date +%Y%m%d-%H%M%S)}"
PROMETHEUS_URL="${PROMETHEUS_URL:-}"
IMAGE_COUNT="${IMAGE_COUNT:-3}"
VUS="${VUS:-4}"
DURATION="${DURATION:-30s}"
CONTROL_RATE="${CONTROL_RATE:-5}"
REQUEST_TIMEOUT="${REQUEST_TIMEOUT:-90s}"

uuid() {
  if command -v uuidgen >/dev/null; then
    uuidgen | tr '[:upper:]' '[:lower:]'
  else
    python3 -c 'import uuid; print(uuid.uuid4())'
  fi
}

command -v k6 >/dev/null || { echo "k6 is required" >&2; exit 1; }
test -f "${IMAGE_FILE}" || { echo "IMAGE_FILE does not exist: ${IMAGE_FILE}" >&2; exit 1; }
mkdir -p "${RESULT_DIR}"
benchmark_failed=0
WINDOWS_FILE="${RESULT_DIR}/grafana-windows.tsv"
printf 'version\tstarted_at_kst\tended_at_kst\tstarted_at_utc\tended_at_utc\tstart_epoch\tend_epoch\n' > "${WINDOWS_FILE}"

if [[ -n "${EXPECTED_PROCESSOR_MODE:-}" ]]; then
  command -v jq >/dev/null || { echo "jq is required for readiness verification" >&2; exit 1; }
  readiness="$(curl --fail --silent --show-error \
    -H "X-Benchmark-Token: ${BENCHMARK_TOKEN}" \
    "${BASE_URL}/api/v1/image-uploads/benchmark-readiness")"
  actual_mode="$(jq -r '.data.processorMode' <<<"${readiness}")"
  actual_delay="$(jq -r '.data.stubAverageDelayMillis' <<<"${readiness}")"
  actual_concurrency="$(jq -r '.data.maxConcurrentRemoteCalls' <<<"${readiness}")"
  actual_virtual_concurrency="$(jq -r '.data.virtualMaxConcurrentTasks' <<<"${readiness}")"
  actual_queue_capacity="$(jq -r '.data.platformQueueCapacity' <<<"${readiness}")"
  [[ "${actual_mode}" == "${EXPECTED_PROCESSOR_MODE}" ]] || {
    echo "processor mode mismatch: expected=${EXPECTED_PROCESSOR_MODE} actual=${actual_mode}" >&2
    exit 2
  }
  if [[ -n "${EXPECTED_STUB_AVERAGE_DELAY_MS:-}" \
      && "${actual_delay}" != "${EXPECTED_STUB_AVERAGE_DELAY_MS}" ]]; then
    echo "stub delay mismatch: expected=${EXPECTED_STUB_AVERAGE_DELAY_MS} actual=${actual_delay}" >&2
    exit 2
  fi
  if [[ -n "${EXPECTED_MAX_CONCURRENT_REMOTE_CALLS:-}" \
      && "${actual_concurrency}" != "${EXPECTED_MAX_CONCURRENT_REMOTE_CALLS}" ]]; then
    echo "remote concurrency mismatch: expected=${EXPECTED_MAX_CONCURRENT_REMOTE_CALLS} actual=${actual_concurrency}" >&2
    exit 2
  fi
  if [[ -n "${EXPECTED_PLATFORM_QUEUE_CAPACITY:-}" \
      && "${actual_queue_capacity}" != "${EXPECTED_PLATFORM_QUEUE_CAPACITY}" ]]; then
    echo "platform queue mismatch: expected=${EXPECTED_PLATFORM_QUEUE_CAPACITY} actual=${actual_queue_capacity}" >&2
    exit 2
  fi
  if [[ -n "${EXPECTED_VIRTUAL_MAX_CONCURRENT_TASKS:-}" \
      && "${actual_virtual_concurrency}" != "${EXPECTED_VIRTUAL_MAX_CONCURRENT_TASKS}" ]]; then
    echo "virtual concurrency mismatch: expected=${EXPECTED_VIRTUAL_MAX_CONCURRENT_TASKS} actual=${actual_virtual_concurrency}" >&2
    exit 2
  fi
  echo "readiness: mode=${actual_mode} delay=${actual_delay}ms platform=${actual_concurrency} virtual=${actual_virtual_concurrency} queue=${actual_queue_capacity}"
fi

VERSIONS="${VERSION_ORDER:-v1 v2 v3}"
for version in ${VERSIONS}; do
  case "${version}" in
    v1|v2|v3) ;;
    *) echo "invalid VERSION_ORDER entry: ${version}" >&2; exit 2 ;;
  esac
  echo "[${version}] warm-up"
  image_parts=()
  for ((index = 0; index < IMAGE_COUNT; index += 1)); do
    image_parts+=( -F "images=@${IMAGE_FILE}" )
  done
  curl --fail --silent --show-error \
    -H "X-Benchmark-Token: ${BENCHMARK_TOKEN}" \
    -F "requestId=$(uuid)" \
    -F "failurePoint=NONE" \
    "${image_parts[@]}" \
    "${BASE_URL}/api/${version}/image-uploads" >/dev/null

  started_at="$(date +%s)"
  started_at_kst="$(date '+%Y-%m-%d %H:%M:%S %z')"
  started_at_utc="$(date -u '+%Y-%m-%dT%H:%M:%SZ')"
  echo "[${version}] k6"
  if ! K6_WEB_DASHBOARD=true \
    K6_WEB_DASHBOARD_EXPORT="${RESULT_DIR}/${version}-report.html" \
    k6 run --quiet \
    -e BASE_URL="${BASE_URL}" \
    -e BENCHMARK_TOKEN="${BENCHMARK_TOKEN}" \
    -e IMAGE_FILE="${IMAGE_FILE}" \
    -e IMAGE_COUNT="${IMAGE_COUNT}" \
    -e VERSION="${version}" \
    -e VUS="${VUS}" \
    -e DURATION="${DURATION}" \
    -e CONTROL_RATE="${CONTROL_RATE}" \
    -e REQUEST_TIMEOUT="${REQUEST_TIMEOUT}" \
    -e SUMMARY_PATH="${RESULT_DIR}/${version}-summary.json" \
    "${SCRIPT_DIR}/k6/image-upload.js"; then
    benchmark_failed=1
    echo "[${version}] k6 thresholds failed; continuing to preserve comparison artifacts" >&2
  fi
  ended_at="$(date +%s)"
  ended_at_kst="$(date '+%Y-%m-%d %H:%M:%S %z')"
  ended_at_utc="$(date -u '+%Y-%m-%dT%H:%M:%SZ')"
  printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\n' \
    "${version}" "${started_at_kst}" "${ended_at_kst}" \
    "${started_at_utc}" "${ended_at_utc}" "${started_at}" "${ended_at}" \
    >> "${WINDOWS_FILE}"

  if [[ -n "${PROMETHEUS_URL}" ]]; then
    python3 "${SCRIPT_DIR}/collect_prometheus.py" \
      --prometheus "${PROMETHEUS_URL}" \
      --start "${started_at}" \
      --end "${ended_at}" \
      --version "${version}" \
      --output "${RESULT_DIR}/${version}-prometheus.json"
  fi
done

python3 "${SCRIPT_DIR}/report.py" \
  --results "${RESULT_DIR}" \
  --base-url "${BASE_URL}" \
  --image-file "${IMAGE_FILE}" \
  --image-count "${IMAGE_COUNT}" \
  --vus "${VUS}" \
  --duration "${DURATION}" \
  --control-rate "${CONTROL_RATE}"
echo "evidence: ${RESULT_DIR}/evidence.md"
echo "chart:    ${RESULT_DIR}/latency.svg"
echo "windows:  ${WINDOWS_FILE}"
exit "${benchmark_failed}"
