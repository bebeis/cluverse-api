# 로컬맵 V1 동기 저장 / V2 비동기 후처리 측정 도구

V1은 provider 재검증을 게시글 저장 트랜잭션 안에서 수행한다. V2는 게시글과 메타데이터만 커밋해 응답한 뒤 `AFTER_COMMIT` 비동기 이벤트에서 provider를 호출하고, 성공 결과를 별도의 짧은 트랜잭션으로 연결한다. 실제 네이버 API는 호출하지 않고 애플리케이션 내부 STUB에 동일한 지연을 주입한다.

기본 비교 조건은 단일 애플리케이션 인스턴스, HikariCP 5개, provider 지연 300ms다. 낮은 RPS부터 부하를 올리며 서비스 SLO인 `p99 < 1초`를 만족하는 최대 지속 RPS를 V1/V2 각각 측정한다. 처리량과 응답시간은 미리 가정하지 않고 반복 측정값만 결과에 기록한다.

## 안전 장치

- `run.sh`와 k6 `setup()`이 모두 readiness를 확인한다.
- `providerMode=STUB`, `experimentEndpointsEnabled=true`, `stubProvider=true` 중 하나라도 아니면 즉시 중단한다.
- V1 실험 API는 `X-Benchmark-Token`이 필요하고 실제 네이버 host를 향하면 애플리케이션 시작이 실패한다.
- `run.sh`는 양의 정수 RPS만 허용한다.
- 핵심 API SLO는 `p99 < 1000ms`, 성공률 99% 이상, dropped iteration 0건이다.
- V2의 k6 응답시간은 게시글 커밋까지만 측정한다. 비동기 provider와 완료 저장은 Grafana의 별도 패널에서 확인한다.

## 1. provider mock 설정

애플리케이션은 다음 설정으로 실행한다. `STUB` 모드에서는 네트워크 클라이언트를 생성하지 않고 고정 장소 후보와 제어 가능한 지연을 반환하는 내부 mock을 사용한다. 토큰 비밀값은 예시를 그대로 운영에 사용하지 않는다.

```bash
LOCAL_MAP_PROVIDER_MODE=STUB \
LOCAL_MAP_SELECTION_TOKEN_SECRET=local-map-benchmark-secret-at-least-32-bytes \
LOCAL_MAP_BENCHMARK_TOKEN=local-map-benchmark \
LOCAL_MAP_EXPERIMENT_ENDPOINTS_ENABLED=true \
./gradlew bootRun
```

mock 호출 수와 지연은 benchmark readiness로 확인한다. V1은 저장 트랜잭션 안에서, V2는 게시글 커밋 이후 비동기 스레드에서 저장 건별 mock 검색을 수행한다. `run.sh`는 실행 직전에 보호된 benchmark 엔드포인트로 지연값과 호출 수를 초기화하고 V2 비동기 호출이 실제 발생했는지도 확인한다.

## 2. smoke와 부하 테스트

인증된 테스트 회원의 memberId와 쓰기 가능한 게시판 ID가 필요하다.

```bash
export AUTH_TOKEN=1 # memberId
export BENCHMARK_TOKEN='local-map-benchmark'
export BOARD_ID=1

script/local-map/smoke.sh
STUB_DELAY_MS=300 script/local-map/run.sh v1
STUB_DELAY_MS=300 script/local-map/run.sh v2
script/local-map/run.sh correctness
```

단일 실행 기본값은 20 RPS, 30초, provider 지연 300ms다. summary JSON과 HTML, Grafana 지표에서 응답과 DB 자원 변화를 함께 확인한다. V1 실행 뒤 생성 데이터를 정리하고 같은 초기 상태에서 V2를 측정한다.

AWS 테스트 스택은 다음 조건으로 맞춘다.

```text
ecs_desired_count     = 1
local_map_db_pool_size = 5
```

여러 ECS 태스크의 풀을 합산하거나 풀 크기를 바꾼 결과를 같은 표에 섞지 않는다.

## 3. SLO 최대 지속 RPS 탐색

각 RPS를 30초씩 3회 반복한다. 세 번 모두 `p99 < 1초`, 성공률 99% 이상, dropped iteration 0건을 만족해야 통과로 채택한다. V2는 응답을 빠르게 반환하는 것만으로 통과시키지 않고, 측정 종료 후 비동기 이벤트가 모두 provider 호출 단계에 도달했는지도 확인한다.

```bash
STUB_DELAY_MS=300 START_RATE=5 STEP_RATE=5 MAX_RATE=50 \
REPETITIONS=3 DURATION=30s script/local-map/run-capacity.sh v1

STUB_DELAY_MS=300 START_RATE=20 STEP_RATE=10 MAX_RATE=150 \
REPETITIONS=3 DURATION=30s script/local-map/run-capacity.sh v2
```

첫 실패 구간을 찾은 뒤 `STEP_RATE=1`로 다시 실행해 경계를 좁힌다. 최종 수치는 인접 실패 RPS와 함께 기록한다.

## 4. 측정값

- k6: `local_map_write_duration` 평균·p95·p99, 성공률, 실제 처리량
- Prometheus/Grafana: `local_map_write_transaction_duration_seconds` p95·p99
- Prometheus/Grafana: `hikaricp_connections_active`, `hikaricp_connections_pending` 최대값
- Prometheus/Grafana: `local_map_place_async_duration_seconds`의 provider·completion p95·p99
- k6: `dropped_iterations`; 1건이라도 있으면 해당 RPS는 실패
- SQL 게이트: fingerprint 중복, requestId 중복, 고아 연결이 모두 0행

`explain/`은 지도 집계와 장소 콘텐츠 조회의 실행 계획을 확인한다. 조회 성능은 이번 V1/V2 비교 변수가 아니지만, 원본 연결 집계가 병목이 되는지 판단할 근거로 남긴다.

k6 summary JSON과 Prometheus CSV를 인기글·조회수 급상승 결과와 함께 matplotlib로 그리려면 다음을 실행한다.

```bash
script/measurements/run.sh
```

Prometheus의 DB 트랜잭션 p95/p99와 Hikari 최대값은
`script/measurements/csv/TEMPLATE.csv` 형식으로 저장한다. 구체적인 예시는
`script/measurements/csv/local-map-example.csv`에 있다.
