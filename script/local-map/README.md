# 로컬맵 V1/V2 측정 도구

V1의 트랜잭션 내부 외부 호출과 V2의 DB 전용 트랜잭션을 같은 배포에서 비교한다. 성능 부하는 항상 로컬 provider stub으로만 실행한다. 실제 네이버 API는 계약 확인용 canary 외에는 호출하지 않는다.

## 안전 장치

- `run.sh`와 k6 `setup()`이 모두 readiness를 확인한다.
- `providerMode=STUB`, `experimentEndpointsEnabled=true`, `stubProvider=true` 중 하나라도 아니면 즉시 중단한다.
- V1 실험 API는 `X-Benchmark-Token`이 필요하고 실제 네이버 host를 향하면 애플리케이션 시작이 실패한다.
- 부하 테스트의 실제 네이버 호출 목표값은 항상 `0회`다.
- 실제 연동 canary는 기본 1 QPS·1회, 최대 5 QPS·10회이며 자동 재시도하지 않는다.
- 네이버의 공개 상한인 10 QPS는 목표치가 아니라 절대 넘지 말아야 할 상한으로만 취급한다.

## 1. provider stub 실행

```bash
STUB_DELAY_MS=300 node script/local-map/provider-stub/server.mjs
```

애플리케이션은 다음 설정으로 실행한다. 토큰 비밀값은 예시를 그대로 운영에 사용하지 않는다.

```bash
LOCAL_MAP_PROVIDER_MODE=STUB \
NAVER_LOCAL_SEARCH_BASE_URL=http://127.0.0.1:19090 \
LOCAL_MAP_SELECTION_TOKEN_SECRET=local-map-benchmark-secret-at-least-32-bytes \
LOCAL_MAP_BENCHMARK_TOKEN=local-map-benchmark \
LOCAL_MAP_EXPERIMENT_ENDPOINTS_ENABLED=true \
./gradlew bootRun
```

stub 호출 수는 `curl http://127.0.0.1:19090/_metrics`로 확인한다. V1은 저장마다 재검색 호출이 증가하고 V2는 최초 검색 이후 저장에서 증가하지 않아야 한다.

## 2. smoke와 부하 테스트

인증된 테스트 회원의 memberId와 쓰기 가능한 게시판 ID가 필요하다.

```bash
export AUTH_TOKEN=1 # memberId
export BENCHMARK_TOKEN='local-map-benchmark'
export BOARD_ID=1

script/local-map/smoke.sh
RATE=5 DURATION=30s script/local-map/run.sh v1
RATE=5 DURATION=30s script/local-map/run.sh v2
script/local-map/run.sh correctness
```

V1과 V2는 같은 RATE, DURATION, 애플리케이션/DB 자원, stub 지연으로 실행한다. V1 실행 뒤 생성 데이터를 정리하고 같은 초기 상태에서 V2를 측정한다.

## 3. 측정값

- k6: `local_map_write_duration` p95·p99, 성공률, 처리량
- Prometheus/Grafana: `local_map_write_transaction_duration_seconds` p95·p99
- Prometheus/Grafana: `hikaricp_connections_active`, `hikaricp_connections_pending` 최대값
- SQL 게이트: fingerprint 중복, requestId 중복, 고아 연결이 모두 0행

`explain/`은 지도 집계와 장소 콘텐츠 조회의 실행 계획을 확인한다. 조회 성능은 이번 V1/V2 비교 변수가 아니지만, 원본 연결 집계가 병목이 되는지 판단할 근거로 남긴다.

k6 summary JSON과 Prometheus CSV를 인기글·조회수 급상승 결과와 함께 matplotlib로 그리려면 다음을 실행한다.

```bash
script/measurements/run.sh
```

Prometheus의 DB 트랜잭션 p95/p99와 Hikari 최대값은
`script/measurements/csv/TEMPLATE.csv` 형식으로 저장한다. 구체적인 예시는
`script/measurements/csv/local-map-example.csv`에 있다.

## 4. 실제 네이버 계약 canary

실제 연동은 지연이나 처리량을 측정하지 않고 인증 헤더, 한글 검색, 응답 매핑, 좌표 변환, 선택 토큰 발급만 확인한다. 실행 중 비-200 응답이 한 번이라도 발생하면 재시도 없이 중단한다. 애플리케이션은 네이버 429도 502 외부 서비스 오류로 변환하므로 canary는 모든 비-200을 동일하게 즉시 중단한다.

```bash
CONFIRM_NAVER_CANARY=YES \
AUTH_TOKEN=1 \
CALLS=1 QPS=1 \
script/local-map/real-provider-smoke.sh
```

스크립트는 CI 실행을 거부하고 날짜별 로컬 장부로 하루 최대 10회만 허용한다. 응답 본문과 selectionToken은 파일에 저장하지 않는다.
