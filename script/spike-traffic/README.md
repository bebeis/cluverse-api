# devlog-10 스파이크 트래픽 측정

Cluverse 핵심 API의 SLO를 먼저 고정하고, 단계형 부하와 스파이크 부하에서 최대 지속 가능 TPS와 병목 이동을 기록한다. 측정 전부터 DB·Redis·JVM 중 하나를 원인으로 정하지 않는다.

한 번의 실행은 다음 증거를 같은 `run_id` 디렉터리에 남긴다.

1. k6 HTML·summary JSON·원시 시계열 CSV
2. Prometheus query_range 원본 JSON·정규화 CSV
3. SLO 단계별 판정 CSV
4. matplotlib PNG
5. 캡처용 단일 HTML 보고서

외부 지도·공공 API와 이미지 업로드는 혼합 TPS에서 제외한다. 호출 쿼터와 파일 크기처럼 다른 변수가 결과에 섞이기 때문이다. 지도 API는 `script/local-map`, 자격시험 공공 API는 `script/certification`의 HTTP stub 기반 시나리오로 분리한다.

## 준비

필수 도구는 `k6`, `jq`, Python 3이다. PNG 생성에는 matplotlib이 필요하다.

```bash
python3 -m venv .venv-spike-traffic
source .venv-spike-traffic/bin/activate
python3 -m pip install -r script/spike-traffic/requirements.txt
```

AWS test 환경에서는 기존 터널로 Grafana와 Prometheus를 연다.

```bash
script/aws/tunnel.sh start
script/spike-traffic/grafana/provision.sh
```

기본 주소는 Grafana `http://localhost:3000`, Prometheus `http://localhost:9090`이다.

## 부하 대상

기본 요청 조합은 다음과 같다. 비율은 운영 로그가 없는 상태의 가정이므로 실제 로그가 생기면 변경한다.

| flow | 기본 비율 | API | 특성 |
|---|---:|---|---|
| 홈 최근 댓글 글 | 15 | `GET /api/v3/home/recent-commented-posts` | 읽기 |
| 인기글 | 10 | `GET /api/v2/popular-posts/recent` | 읽기 |
| 게시글 목록 | 25 | `GET /api/v4/posts` | 읽기 |
| 게시글 상세 | 20 | `GET /api/v1/posts/{postId}` | 읽기 SLI, 조회수 변경 |
| 댓글 목록 | 15 | `GET /api/v2/comments` | 읽기 |
| 조회수 반영 | 10 | `POST /api/v4/posts/{postId}/view-count` | 쓰기 |
| 댓글 작성 | 5 | `POST /api/v1/comments` | 쓰기 |

`POST_IDS`의 앞쪽 `HOT_POST_COUNT`개에는 `HOT_POST_SHARE` 비율로 요청을 집중한다. 기본값은 앞쪽 10개에 80%다. 모든 ID를 균등하게 호출하는 것보다 커뮤니티의 인기 데이터 집중을 가깝게 표현한다.

게시글 상세도 조회수를 변경하고 댓글 쓰기는 행을 생성한다. 반드시 재적재 가능한 test fixture에서 실행하며 `ALLOW_DATA_MUTATION=1`을 명시해야 한다. 원격 주소에는 추가로 `CONFIRM_LOAD_TEST=1`이 필요하다.

## 실행 순서

### 사전 확인

짧은 부하로 ID·권한·응답 계약을 먼저 확인한다.

```bash
ALLOW_DATA_MUTATION=1 \
MEMBER_IDS=1,2,3 \
BOARD_IDS=1,2 \
POST_IDS=1,2,3,4,5 \
script/spike-traffic/run.sh smoke
```

smoke에서도 쓰기를 피하려면 상세·쓰기 가중치를 모두 0으로 두고 나머지 읽기 가중치만 남긴다.

### 용량 경계

낮은 도착률부터 단계적으로 높이며 SLO가 처음 깨지는 구간을 찾는다.

```bash
ALLOW_DATA_MUTATION=1 \
CONFIRM_LOAD_TEST=1 \
BASE_URL=https://test-api.example.com \
PROMETHEUS_URL=http://localhost:9090 \
MEMBER_IDS=1,2,3,4,5 \
BOARD_IDS=1,2,3 \
POST_IDS=100,101,102,103,104,105,106,107,108,109,110 \
CAPACITY_RATES=25,50,100,150,200 \
STEP_DURATION_SECONDS=600 \
LABEL=baseline \
DATASET_LABEL=post-30m-comment-10m \
CACHE_CONDITION=warm \
APP_SPEC='2 vCPU / 4 GiB' \
DB_SPEC='2 vCPU / 8 GiB' \
LOAD_GENERATOR_SPEC='4 vCPU / 8 GiB' \
script/spike-traffic/run.sh capacity
```

각 단계는 기본 10분간 유지하고 첫 30초는 이전 단계의 영향을 줄이기 위해 SLO 판정에서 제외한다. `STEP_DURATION_SECONDS`와 `STEP_SETTLE_SECONDS`로 바꿀 수 있다. 2분 정도의 짧은 단계는 한계 구간을 탐색하는 예비 측정에는 사용할 수 있지만, 블로그의 최대 지속 가능 TPS는 10분 재측정 결과로 확정한다. 최대 지속 가능 TPS는 다음 조건을 모두 만족한 가장 높은 단계다.

- 완료 RPS가 목표 TPS의 98% 이상
- 읽기·쓰기 p95와 p99 SLO 만족
- 성공률 SLO 만족
- `dropped_iterations == 0`

### 스파이크

용량 경계를 기준으로 평상시와 피크 도착률을 정한다.

```bash
ALLOW_DATA_MUTATION=1 \
CONFIRM_LOAD_TEST=1 \
BASE_URL=https://test-api.example.com \
PROMETHEUS_URL=http://localhost:9090 \
MEMBER_IDS=1,2,3,4,5 \
BOARD_IDS=1,2,3 \
POST_IDS=100,101,102,103,104,105,106,107,108,109,110 \
NORMAL_RATE=50 \
SPIKE_RATE=250 \
BASELINE_SECONDS=120 \
RAMP_SECONDS=10 \
SPIKE_SECONDS=120 \
RECOVERY_SECONDS=120 \
LABEL=baseline-spike \
script/spike-traffic/run.sh spike
```

정상화 소요 시간은 부하가 평상시 수준으로 돌아온 뒤 읽기·쓰기 p99, 성공률, dropped iteration이 30초 연속 SLO를 만족하기 시작한 시점까지다. `NORMALIZATION_WINDOW_SECONDS`로 안정 구간을 조정한다.

## SLO와 가중치

| 환경 변수 | 기본값 |
|---|---:|
| `READ_P95_MS` / `READ_P99_MS` | 300 / 800 |
| `WRITE_P95_MS` / `WRITE_P99_MS` | 500 / 1500 |
| `SUCCESS_RATE` | 0.999 |
| `WEIGHT_HOME_RECENT` | 15 |
| `WEIGHT_POPULAR_POSTS` | 10 |
| `WEIGHT_POST_LIST` | 25 |
| `WEIGHT_POST_DETAIL` | 20 |
| `WEIGHT_COMMENT_LIST` | 15 |
| `WEIGHT_VIEW_COUNT` | 10 |
| `WEIGHT_COMMENT_WRITE` | 5 |

가중치 합이 꼭 100일 필요는 없다. k6가 전체 합에 대한 상대 비율로 선택한다. 개선 전후에는 SLO, 가중치, fixture, 인프라 사양과 캐시 초기 조건을 동일하게 유지한다.

## 병목 관측

Grafana 대시보드는 캡처를 위한 세 행으로 구성한다.

- SLI/SLO: completed RPS, HTTP p95·p99, 5xx
- 애플리케이션 압력: Hikari active·pending, connection acquire·usage, Tomcat thread, process CPU
- 저장소 압력: MySQL QPS·running thread·CPU·row lock wait, Redis command·hit ratio

모든 지표를 블로그에 싣지 않는다. SLO가 무너진 시각과 함께 변했고, 통제 실험으로 원인과 연결된 지표만 캡처한다. Prometheus 수집 결과는 `prometheus-timeseries.csv`에 남으므로 Grafana 화면과 matplotlib가 같은 원본을 사용할 수 있다.

## EXPLAIN ANALYZE

느린 API를 확인한 뒤 해당 API의 실제 SQL과 대표 파라미터를 템플릿에 옮긴다.

```bash
cp script/spike-traffic/explain/queries/TEMPLATE.sql /tmp/post-list-explain.sql
MYSQL_HOST=127.0.0.1 \
MYSQL_PORT=3306 \
MYSQL_USER=cluverse \
MYSQL_DATABASE=cluverse \
MYSQL_PWD='...' \
script/spike-traffic/explain/capture.sh /tmp/post-list-explain.sql post-list
```

`capture.sh`는 `EXPLAIN ANALYZE`가 없는 파일이나 데이터 변경 SQL을 거부한다. 출력은 다음과 같다.

- `explain-plan.txt`: MySQL 원문
- `explain-nodes.csv`: 노드별 actual time, rows, loops
- `explain-summary.json`: sort·temporary·table scan과 대표값
- `explain-report.html`: 실행 계획 캡처 화면

노드별 `output_rows`는 해당 연산자가 출력한 `rows × loops`다. 실행 계획 전체에서 이를 더한 값을 “총 방문 행”으로 표현하지 않는다. 부모·자식 연산자의 행이 중복되기 때문이다.

## 결과와 비교

실행별 결과는 `script/spike-traffic/results/raw/<run-id>`에 저장된다.

| 파일 | 용도 |
|---|---|
| `k6-report.html` | k6 원본 캡처 |
| `report.html` | SLO 카드·표·그래프를 모은 단일 캡처 화면 |
| `spike-overview.png` | offered/completed TPS, p99, 실패·dropped의 공통 시간축 |
| `capacity-curve.png` | 목표 TPS별 완료 RPS와 SLO 붕괴 지점 |
| `bottleneck-signals.png` | Prometheus 병목 후보 |
| `k6-timeseries.csv` | 5초 버킷 정규화 시계열 |
| `slo-steps.csv` | 부하 단계별 SLO 판정 |
| `analysis-summary.json` | 실행 비교용 요약 |

matplotlib 설치 전 실행했다면 원시 결과는 유지된다. 설치 후 다시 렌더링한다.

```bash
script/spike-traffic/render.sh script/spike-traffic/results/raw/<run-id>
```

병목 개선 전후 실행은 `LABEL`로 구분하고 비교한다.

```bash
python3 script/spike-traffic/compare_runs.py \
  --run-dir script/spike-traffic/results/raw/<baseline-run> \
  --run-dir script/spike-traffic/results/raw/<after-run> \
  --output-dir script/spike-traffic/results/comparison
```

`comparison.html`과 `comparison.png`는 실행별 최대 지속 가능 TPS를 비교하되, tail latency는 모든 실행에 공통으로 존재하는 동일한 TPS 단계에서 비교한다. 더 높은 부하를 견딘 개선 구조의 p99를 기준선의 낮은 부하 p99와 직접 비교하는 오류를 피하기 위해서다. 블로그에는 `baseline`, `query optimization`, `queue bound`처럼 실제로 적용한 변경을 label로 사용하고, 측정 전에 개선 단계를 미리 만들지 않는다.

matplotlib 축이 실행 환경의 한글 폰트에 의존하지 않도록 `LABEL`은 짧은 영문 표현을 권장한다. 상세 설명은 HTML 보고서나 측정 기록 Markdown에 한글로 남긴다.

## 측정 주의사항

- k6 부하 발생기의 CPU·네트워크와 `dropped_iterations`를 먼저 확인한다.
- 개선 전후에는 한 번에 한 변수만 변경한다.
- cache cold와 warm 결과를 섞지 않는다.
- 외부 API에 이 부하를 전달하지 않는다.
- threshold 실패로 k6가 종료 코드 0이 아니어도 raw·Prometheus·그래프 수집은 계속한다.
- 원시 JSON·CSV·콘솔 출력은 결과 해석이 끝날 때까지 보존한다.
