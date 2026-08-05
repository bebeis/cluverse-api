#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
BENCHMARK_TOKEN="${BENCHMARK_TOKEN:?BENCHMARK_TOKEN is required}"
IMAGE_FILE="${IMAGE_FILE:?IMAGE_FILE is required}"
FAILURE_POINT="${FAILURE_POINT:-AFTER_FIRST_OBJECT}"
VERSION="${VERSION:-v2}"

if command -v uuidgen >/dev/null; then
  REQUEST_ID="$(uuidgen | tr '[:upper:]' '[:lower:]')"
else
  REQUEST_ID="$(python3 -c 'import uuid; print(uuid.uuid4())')"
fi

status="$(curl --silent --output /dev/null --write-out '%{http_code}' \
  -H "X-Benchmark-Token: ${BENCHMARK_TOKEN}" \
  -F "requestId=${REQUEST_ID}" \
  -F "failurePoint=${FAILURE_POINT}" \
  -F "images=@${IMAGE_FILE}" \
  "${BASE_URL}/api/${VERSION}/image-uploads")"

echo "version=${VERSION} request_id=${REQUEST_ID} failure_point=${FAILURE_POINT} http_status=${status}"
echo "known failure는 FAILED, REMOTE_TIMEOUT은 재조정 전 PENDING이어야 한다. consistency.sql로 확인한다."
