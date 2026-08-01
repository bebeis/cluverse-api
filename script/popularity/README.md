# 인기글 승격 구조 성능 측정 도구

동일한 인기글 정책을 실행하는 두 경로를 한 번의 배포에서 비교합니다.

| 코드 식별자 | 실행 API | 측정 대상 |
|---|---|---|
| V1 | `POST /api/v1/popular-posts/promotion-runs` | 최근 48시간 게시글 전체 집계 |
| V2 | `POST /api/v2/popular-posts/{postId}/promotion-checks` | 변경된 게시글의 증분 판정 |

V1과 V2의 요청 하나는 처리 단위가 다릅니다. 단순 요청 지연만 비교하지 않고, 서버 메트릭과 DB 통계를 함께 사용해 `소스 이벤트 1,000건` 또는 `승격 1건` 기준으로 정규화합니다. 측정 전에는 두 경로의 승격 결과 집합이 같은지 먼저 확인합니다.

## 디렉터리

```text
script/popularity/
├── run.sh
├── smoke.sh
├── k6/
│   ├── popularity-bench.k6.js
│   ├── popularity-lifecycle.k6.js
│   └── popularity-recall.k6.js
├── explain/
├── seed/
├── grafana/
└── results/
```

`run.sh`는 k6 웹 대시보드와 HTML 리포트, summary JSON을 `results/raw/`에 저장합니다. `smoke.sh`는 실험 API가 열렸는지와 두 URL 버전의 기본 계약을 확인합니다. `seed/fixture.sql`은 전용 성능 측정 DB에서만 사용해야 합니다.

summary JSON을 조회수 급상승·로컬맵 결과와 함께 matplotlib 그래프로 모으려면
`script/measurements/run.sh`를 실행합니다. 공통 CSV 입력 형식은
`script/measurements/README.md`에 정리되어 있습니다.

V1 기준선의 주기 실행은 API 노출과 별도인 `popularity.baseline-scan-enabled`로 제어합니다. 수동 k6 비교만 수행할 때는 기본값 `false`를 유지합니다.

## 사전 조건

- k6 0.50 이상
- `curl`, `jq`
- 애플리케이션 설정 `popularity.experiment-endpoints-enabled=true`
- `popularity.benchmark-token`에 비어 있지 않은 벤치마크 토큰 설정
- 데이터 규모, 정책값, MySQL 버퍼 풀과 인프라 사양을 고정한 측정 환경

기본 헤더는 `X-Benchmark-Token`입니다. 서버 설정과 다르면 `BENCHMARK_HEADER`를 바꿉니다.

```bash
export BASE_URL=http://localhost:8080
export BENCHMARK_TOKEN='<token>'
export BENCHMARK_HEADER=X-Benchmark-Token
```

## 빠른 검증

```bash
script/popularity/smoke.sh
```

기본 `POST_ID`는 fixture의 `9100000001`입니다. fixture를 쓰지 않으면 현재 DB에 존재하는 ACTIVE 게시글로 바꿉니다.

```bash
POST_ID=1234 script/popularity/smoke.sh
```

고정 fixture를 명시적으로 적재하려면 전용 DB에서만 다음처럼 실행합니다.

```bash
mysql -h127.0.0.1 -ucluverse_user -ptest1234 cluverse_v2 \
  < script/popularity/seed/fixture.sql
```

fixture는 고정 ID `9100000000`~`9100000005`에 전용 회원·게시판·게시글과 카운트를 만듭니다. 운영·공용 개발 DB에서는 실행하지 않습니다. 원복은 `seed/reset.sql`로 이 고정 ID 범위만 제거합니다. fixture 적재 전에 애플리케이션이 실행 중이었다면 정책 메모리 캐시를 비우기 위해 재시작하거나 캐시 갱신 주기를 기다립니다.

## 기존 게시글 인덱스 배포

`V2__create_popularity_tables.sql`은 기존 `post` 테이블에 `idx_post_popularity_scan`을 추가합니다. 운영 반영 전 운영과 같은 데이터 규모의 복제본에서 인덱스 생성 시간을 측정하고, 그 결과를 기준으로 저트래픽 배포 창을 정합니다.

- 배포 전 장기 트랜잭션과 메타데이터 잠금 대기 여부를 확인합니다.
- 배포 중 `performance_schema.metadata_locks`, `SHOW PROCESSLIST`, 디스크 여유 공간과 redo 로그 증가량을 관찰합니다.
- 허용한 배포 시간을 넘기거나 쓰기 지연이 임계값을 초과하면 Flyway 실행 연결을 종료하고 DDL 롤백 완료를 확인합니다.
- 마이그레이션 완료 뒤 애플리케이션 롤백이 필요하면 `DROP INDEX idx_post_popularity_scan ON post`를 별도 승인된 운영 변경으로 수행합니다.

실측 시간, 허용 지연과 중단 임계값은 환경마다 다르므로 추정하지 않고 배포 기록에 남깁니다.

## 측정 순서

### 1. 정답 집합 검증

고정 데이터에서 기대 승격 ID를 쉼표로 전달합니다.

```bash
script/popularity/run.sh recall \
  -e EXPECTED_POST_IDS=9100000001,9100000002 \
  -e CHECK_POST_IDS=9100000001,9100000002,9100000003,9100000004,9100000005
```

`popularity_recall_ratio=1`, `popularity_false_promotion_ratio=0`, `popularity_result_set_match=1`이 아니면 성능 결과를 채택하지 않습니다. 조회 API의 응답 범위보다 결과가 많으면 `SIZE`를 늘리거나 DB에서 전체 결과를 별도로 대조합니다.

### 2. V1 전체 집계

```bash
script/popularity/run.sh bench \
  -e VERSION=v1 -e RATE=1 -e DURATION=5m
```

V1의 `RATE`는 초당 전체 집계 실행 수입니다. 짧은 주기 배치의 CPU·I/O 스파이크와 한 주기당 검사 수를 확인합니다.

### 3. V2 증분 판정

```bash
script/popularity/run.sh bench \
  -e VERSION=v2 -e RATE=300 -e DURATION=5m \
  -e POST_ID_MIN=5000001 -e POST_ID_MAX=5999999
```

V2의 `RATE`는 초당 변경 이벤트 수입니다. 두 경로의 RATE를 같게 두는 것이 공정한 비교가 아닙니다. 결과 문서에서 실제 이벤트 수, 논리적 검사 수, 승격 수로 정규화합니다.

### 4. 상태 수명 관찰

```bash
script/popularity/run.sh lifecycle \
  -e POST_ID=9100000003 -e EVENT_RATE=20 -e DURATION=10m
```

이 스크립트는 승격 검사를 반복하고 최근 인기글 노출을 함께 관찰합니다. 좋아요·댓글·조회수 값을 직접 만들지 않습니다. fixture, 애플리케이션 API 또는 별도 트래픽으로 조건 충족 시점을 만들어야 합니다. 조건 충족 시각, 후보 등록, 승격, 최종화 시각은 Grafana와 DB 스냅샷을 함께 확인합니다.

## EXPLAIN

```bash
mysql -h127.0.0.1 -ucluverse_user -ptest1234 cluverse_v2 \
  < script/popularity/explain/v1-recent-post-scan.sql
```

각 SQL 상단의 변수를 실제 게시판과 게시글 ID로 바꿉니다. 가능하면 `EXPLAIN ANALYZE`의 `actual rows`와 실제 실행 시간을 기록합니다. 쓰기 쿼리는 데이터를 변경하지 않도록 동일한 읽기 형태나 트랜잭션 롤백 예시로 제공합니다.

`finalization-due-scan.sql`은 최종화 배치의 2단계 조회를 그대로 분리해 측정합니다. 먼저 만기된 고유 `post_id`를 `LIMIT`으로 선택하고, 이어서 선택된 ID의 V1/V2 만기 행을 모두 읽습니다. 두 단계의 actual rows를 따로 기록해야 배치 크기가 알고리즘 행 사이를 가르지 않는지와 추가 조회 비용을 함께 확인할 수 있습니다.

Performance Schema 델타는 다음 순서로 수집합니다.

1. 부하 직전에 `performance-schema-delta.sql`의 기준 스냅샷 블록 실행
2. 부하 수행
3. 같은 세션에서 델타 블록 실행
4. `SUM_ROWS_EXAMINED`, `COUNT_STAR`, 대기 시간 기록

## Grafana와 서버 메트릭

`grafana/popularity-dashboard.json`은 설계된 저카디널리티 지표를 사용합니다. 대시보드의 `DS_PROMETHEUS` datasource 변수에서 Prometheus 인스턴스를 선택합니다.

- `popularity_evaluation_total`
- `popularity_evaluation_duration_seconds`
- `popularity_promotion_total`
- `popularity_examined_candidates_total`
- `popularity_batch_duration_seconds`
- `popularity_candidate_queue_size`
- `popularity_candidate_lag_seconds`
- `popularity_finalization_delay_seconds`
- `popularity_candidate_evaluation_failures_total`
- `popularity_finalization_pending_loss_risk_total`
- `popularity_policy_cache_hit_total`, `popularity_policy_cache_miss_total`

Micrometer의 실제 Prometheus 이름은 Meter 타입과 suffix에 따라 달라질 수 있습니다. `/actuator/prometheus`에서 노출 이름을 확인하고 대시보드 쿼리와 결과 문서에 사용한 이름을 기록합니다.

## 채택 기준

- 워밍업 결과 제외
- 동일한 DB 스냅샷과 정책 사용
- V1/V2 실행 순서 교차
- 기대 승격 집합 일치
- k6 `dropped_iterations`, 에러율, 실측 RPS 기록
- `EXPLAIN` 추정 rows 대신 `EXPLAIN ANALYZE` actual rows 우선
- 앱 지연뿐 아니라 DB CPU·I/O·문장 수 함께 기록
- 측정하지 않은 칸은 추정값으로 채우지 않기

측정 결과는 `results/TEMPLATE.md`를 복사해 기록합니다.

```bash
cp script/popularity/results/TEMPLATE.md \
  script/popularity/results/$(date +%F)-popularity.md
```
