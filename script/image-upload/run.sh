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

for version in v1 v2 v3; do
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
  echo "[${version}] k6"
  if ! k6 run \
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

  if [[ -n "${PROMETHEUS_URL}" ]]; then
    python3 "${SCRIPT_DIR}/collect_prometheus.py" \
      --prometheus "${PROMETHEUS_URL}" \
      --start "${started_at}" \
      --end "${ended_at}" \
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
exit "${benchmark_failed}"
