# 성능 측정 결과 통합·시각화

기존 k6 HTML, summary JSON, Grafana 대시보드와 결과 Markdown은 그대로 유지한다. 이 디렉터리는 인기글, 조회수 급상승, 로컬맵, 댓글 페이지 결과를 공통 CSV로 정규화하고 matplotlib 이미지로 그리는 보조 계층이다.

## 지원 입력

- k6 `--summary-export` JSON
- k6 `--out csv` 시계열 CSV
- `csv/TEMPLATE.csv` 형식의 정규화 CSV

Grafana/Prometheus에서 추출한 지표는 공급자마다 CSV 열 구조가 다르므로 정규화 CSV로 옮긴다. 로컬맵 예시는 `csv/local-map-example.csv`에 있다.

필수 열은 다음 세 개다.

| 열 | 의미 |
|---|---|
| `metric` | `db_transaction_latency`, `hikari_active`처럼 지표를 구분하는 이름 |
| `stat` | `p95`, `p99`, `max`, `rate`, `point` 등 |
| `value` | 숫자 |

`experiment`, `version`, `run_id`, `scenario`, `unit`, `recorded_at`을 함께 넣으면 다른 결과와 안전하게 구분할 수 있다.

## 설치

프로젝트 의존성과 분리된 가상환경 사용을 권장한다.

```bash
python3 -m venv .venv-measurements
source .venv-measurements/bin/activate
python3 -m pip install -r script/measurements/requirements.txt
```

## 실행

세 실험의 `results/raw`를 한 번에 읽는다.

```bash
script/measurements/run.sh
```

특정 파일과 수동 CSV만 선택할 수도 있다.

```bash
python3 script/measurements/plot_results.py \
  --input script/local-map/results/raw/2026-08-01-v1-summary.json \
  --input script/local-map/results/raw/2026-08-01-v2-summary.json \
  --input my-local-map-prometheus.csv \
  --output-dir script/measurements/results/local-map
```

반복 측정값을 그대로 모두 표시하는 것이 기본이다. 동일 조건을 여러 번 실행했다면 중앙값이나 최신 결과로 묶을 수 있다.

```bash
script/measurements/run.sh --aggregate median
script/measurements/run.sh --aggregate latest --format png --format svg
```

조건이 다른 실행을 중앙값으로 합치면 의미가 없으므로, 입력 파일을 먼저 같은 RATE·기간·데이터 스냅샷·인프라 조건으로 좁혀야 한다.

## 출력

- `measurements.csv`: 모든 입력을 공통 long-format CSV로 정규화한 원본
- `<experiment>-latency.png`: 버전·실행별 API p95/p99
- `<experiment>-traffic.png`: 처리량과 실패율
- `<experiment>-steps.png`: 조회수 급상승 계단 부하의 rate별 p95/p99
- `<experiment>-additional-metrics.png`: Prometheus/Grafana에서 보충한 DB·Hikari·Redis 지표
- `comment-pagination-scale-latency.png`: 댓글 수별 개선 전·후 댓글 API와 상세 화면 p95/p99
- `comment-pagination-scale-rows.png`: 댓글 수별 개선 전·후 `actual rows`

이미지 파일명과 축 라벨은 영문으로 고정해 실행 환경의 한글 폰트 유무가 그래프 생성을 깨뜨리지 않게 한다. 원래 숫자와 출처는 항상 `measurements.csv`에 남는다.
