#!/usr/bin/env bash
# URL-prefix V1/V2 실험 API의 최소 계약을 검증한다. DB 상태를 직접 바꾸지 않는다.
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
BENCHMARK_HEADER="${BENCHMARK_HEADER:-X-Benchmark-Token}"
BENCHMARK_TOKEN="${BENCHMARK_TOKEN:-}"
POST_ID="${POST_ID:-9100000001}"

for command in curl jq; do
  if ! command -v "$command" >/dev/null 2>&1; then
    echo "필수 명령을 찾을 수 없습니다: $command" >&2
    exit 1
  fi
done

CURL_HEADERS=(-H "Accept: application/json")
if [[ -n "$BENCHMARK_TOKEN" ]]; then
  CURL_HEADERS+=(-H "$BENCHMARK_HEADER: $BENCHMARK_TOKEN")
fi

request() {
  local method="$1"
  local path="$2"
  local output
  local status

  output="$(mktemp)"
  status="$(curl --silent --show-error --output "$output" --write-out '%{http_code}' \
    --request "$method" "${CURL_HEADERS[@]}" "$BASE_URL$path")"

  if [[ "$status" -lt 200 || "$status" -ge 300 ]]; then
    echo "실패: $method $path -> HTTP $status" >&2
    jq . "$output" 2>/dev/null || sed -n '1,20p' "$output" >&2
    rm -f "$output"
    if [[ "$status" == 404 ]]; then
      echo "실험 엔드포인트 설정(popularity.experiment-endpoints-enabled)을 확인하세요." >&2
    elif [[ "$status" == 401 || "$status" == 403 ]]; then
      echo "BENCHMARK_TOKEN과 BENCHMARK_HEADER를 확인하세요." >&2
    fi
    exit 1
  fi

  if ! jq -e '.code >= 200 and .code < 300' "$output" >/dev/null; then
    echo "ApiResponse 계약 불일치: $method $path" >&2
    jq . "$output" >&2
    rm -f "$output"
    exit 1
  fi
  rm -f "$output"
  echo "통과: $method $path"
}

request POST "/api/v1/popular-posts/promotion-runs"
request POST "/api/v2/popular-posts/$POST_ID/promotion-checks"
request POST "/api/v2/popular-posts/$POST_ID/promotion-checks"
request GET "/api/v1/popular-posts/recent"
request GET "/api/v2/popular-posts/recent"
request GET "/api/v1/popular-posts/history?sort=LATEST"
request GET "/api/v1/popular-posts/history?sort=SCORE"
request GET "/api/v2/popular-posts/history?sort=LATEST"
request GET "/api/v2/popular-posts/history?sort=SCORE"

echo
echo "기본 API 계약이 통과했습니다."
echo "중복 승격 멱등성과 V1/V2 결과 차이는 fixture 적재 후 popular_post를 조회해 확인하세요."
