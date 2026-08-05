# Devlog 6 인기글 판정 측정

한 번의 배포에서 최초 구조와 개선 구조를 URL prefix로 비교한다.

| 버전 | 실행 API | 판정 방식 |
|---|---|---|
| V1 | `POST /api/v1/popular-posts/promotion-runs` | 최근 48시간 글 전체를 읽고 전역 기준으로 선발 |
| V2 | `POST /api/v2/popular-posts/{postId}/promotion-checks` | 좋아요·댓글이 변한 글 하나를 게시판별 기준으로 판정 |

실험 쓰기 API는 `popularity.experiment-endpoints-enabled=true`인 측정 배포에서만 열리고 `X-Benchmark-Token`으로 보호된다.

## 핵심 지표

블로그 본문에는 두 성능 지표와 한 정책 지표만 사용한다.

1. 판정 완료 p99
2. 판정 한 번당 검사한 게시글 수
3. 게시판별 승격률 편차

V1은 하루 한 번 실행되는 배치라 V2와 요청 TPS를 직접 비교하지 않는다. 비교하려는 것은 같은 반응 변화를 발견하기 위해 읽는 범위와 사용자가 기다리는 승격 지연이다.

## fixture

```bash
mysql -h127.0.0.1 -ucluverse_user -p cluverse_v2 < script/popularity/seed/fixture.sql
```

fixture는 전역 기준 100인 V1에서는 바쁜 게시판의 글만, 게시판별 기준을 사용하는 V2에서는 조용한 게시판의 점수 21짜리 글도 승격되도록 구성되어 있다. 조회수는 입력 데이터에 포함되지 않는다.

AWS 측정 환경에서는 `script/aws/seed.sh view-count --wait`와 `full` 프로파일이 이 fixture를
대량 게시글 시드 뒤에 자동으로 적재한다. 위 MySQL 명령은 로컬 실행 또는 fixture만 다시 맞출 때 사용한다.

## 실행

```bash
export BENCHMARK_TOKEN='<측정 배포 토큰>'

# V1 배치는 비싸므로 적은 반복으로 완료 시간과 검사량을 잰다.
script/popularity/run.sh -e VERSION=v1 -e BENCHMARK_TOKEN="$BENCHMARK_TOKEN" -e ITERATIONS=3 -e VUS=1

# V2 단건 판정은 충분한 표본으로 p99를 잰다.
script/popularity/run.sh -e VERSION=v2 -e BENCHMARK_TOKEN="$BENCHMARK_TOKEN" \
  -e POST_ID_MIN=5900000 -e POST_ID_MAX=5999999 -e ITERATIONS=1000 -e VUS=20
```

각 실행은 `results/raw/`에 summary JSON과 캡처용 HTML 대시보드를 만들고, `results/metrics.csv`에 요약 한 행을 추가한다. 조건별로 최소 3회 반복하고 중앙값을 사용한다.

## EXPLAIN ANALYZE

```bash
mysql -h127.0.0.1 -ucluverse_user -p cluverse_v2 < script/popularity/explain/v1-recent-post-scan.sql
mysql -h127.0.0.1 -ucluverse_user -p cluverse_v2 < script/popularity/explain/v2-post-snapshot.sql
mysql -h127.0.0.1 -ucluverse_user -p cluverse_v2 < script/popularity/explain/policy-sample.sql
```

캡처에서 볼 값은 `actual time`, `rows`, `loops`다. estimated cost만으로 개선을 주장하지 않는다.

게시판별 승격률은 다음 결과를 CSV로 저장해 사용한다.

```bash
mysql --batch --raw -h127.0.0.1 -ucluverse_user -p cluverse_v2 \
  < script/popularity/explain/board-promotion-rate.sql \
  > script/popularity/results/board-promotion-rate.tsv
```

## Matplotlib 그래프

```bash
python3 -m pip install -r script/popularity/requirements.txt
python3 script/popularity/plot_results.py
```

`results/popularity-comparison.png`은 판정 p99와 판정당 검사 게시글 수를 두 패널로 만든다. 검사량 차이가 크므로 두 번째 패널은 로그 축을 사용한다.

## 결과 기록

```bash
cp script/popularity/results/TEMPLATE.md script/popularity/results/$(date +%F)-popularity.md
```

좋아요·댓글 저장부터 최근 인기글 조회에 나타날 때까지의 승격 지연은 k6 판정 시간과 DB `promoted_at`을 함께 사용한다. V1은 배치 시작 시각, V2는 상호작용 요청 시각을 기준점으로 삼는다.
