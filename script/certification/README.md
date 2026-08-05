# 자격시험 외부 API 측정 도구

자격시험 홈 컴포넌트의 캐시 동작과 외부 API 실패 처리를 실제 공공데이터포털 대신 로컬 HTTP stub으로 검증한다. 부하 테스트에서 실제 공공 API 호출 목표값은 항상 `0회`다.

## 안전 장치

- 애플리케이션 시작 시 `provider-mode=STUB`, 실험 API 활성화, 실제 공공데이터포털이 아닌 host를 함께 검사한다.
- `run.sh`, `failure-smoke.sh`, k6 `setup()`이 readiness를 중복 확인한다.
- 캐시 초기화 엔드포인트는 실험 모드에서만 활성화되고 `X-Benchmark-Token`으로 보호된다.
- 실제 공공 API 연동은 단위 계약 테스트와 별도로 수행하며, 이 디렉터리에는 실제 공급자 호출 스크립트를 두지 않는다.

## 실행 준비

터미널 1에서 HTTP stub을 실행한다.

```bash
node script/certification/provider-stub/server.mjs
```

터미널 2에서 애플리케이션을 스텁 모드로 실행한다.

```bash
CERTIFICATION_PROVIDER_MODE=STUB \
CERTIFICATION_PROVIDER_BASE_URL=http://127.0.0.1:19091 \
DATA_GO_KR_SERVICE_KEY=stub-key \
CERTIFICATION_EXPERIMENT_ENDPOINTS_ENABLED=true \
CERTIFICATION_BENCHMARK_TOKEN=certification-benchmark \
./gradlew bootRun
```

## 캐시 측정

```bash
MEMBER_ID=1 \
BENCHMARK_TOKEN=certification-benchmark \
script/certification/run.sh warm

MEMBER_ID=1 \
BENCHMARK_TOKEN=certification-benchmark \
BURST_VUS=50 \
script/certification/run.sh cold-burst
```

- `warm`: 캐시를 한 번 예열한 뒤 일정한 도착률로 조회한다.
- `cold-burst`: 캐시가 빈 시점에 동시 요청을 보내 Caffeine의 키별 적재가 중복 외부 호출을 막는지 확인한다.
- 두 프로필 모두 현재 연도와 다음 연도를 한 번씩만 조회하므로 stub 호출 수가 정확히 `2회`인지 검증한다.

결과는 `results/raw/<timestamp>-<profile>` 아래에 k6 summary JSON, 콘솔 원문, provider 호출 횟수 JSON으로 남는다.

## 실패 주입

```bash
MEMBER_ID=1 \
BENCHMARK_TOKEN=certification-benchmark \
script/certification/failure-smoke.sh
```

공급자의 오류 코드, 깨진 JSON, 애플리케이션 읽기 제한보다 긴 지연을 차례로 주입하고 모두 HTTP 502로 격리되는지 확인한다. 각 케이스 전 캐시를 비우므로 이전 성공 응답이 실패를 가리지 않는다.

## 해석 범위

이 테스트의 p95·p99는 캐시된 홈 컴포넌트 조회와 최초 적재 시 요청 병합을 확인하기 위한 값이다. 공공 API 자체의 성능 수치로 해석하지 않는다. 실제 공급자의 지연은 통제할 수 없고 호출 쿼터도 있기 때문에, 외부 네트워크 성능 비교와 애플리케이션 용량 측정을 분리한다.
