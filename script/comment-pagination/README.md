# 댓글 페이지 조회 개선 전·후 측정

같은 댓글 데이터와 응답 계약에서 재귀 CTE 조회와 저장된 `path` 인덱스 조회를 비교한다. API URL은 실행 구분을 위해 `v1`, `v2`를 사용하지만 블로그와 그래프에서는 `Before`, `After`로 표현한다.

## 측정 대상

- 댓글 API p95·p99
- 게시글 본문·댓글 병렬 요청 완료 시간 p95·p99
- `EXPLAIN ANALYZE` actual rows·loops와 temporary table/filesort
- Hikari pending connection 최대값
- 경로 저장이 적용된 루트 댓글·답글 작성 p95·p99

정합성은 성능 수치와 분리한다. 두 조회의 댓글 ID·순서·`hasNext`가 같고, 페이지 전체에 중복이 없으며 커밋된 댓글의 `path` 누락이 0건이어야 한다.

## 사전 조건

- MySQL 8.x와 애플리케이션이 실행 중이어야 한다.
- `POST_ID`는 조회 가능한 게시글이어야 한다.
- fixture를 직접 만들 때는 해당 게시글 작성자 또는 테스트 회원의 `MEMBER_ID`가 필요하다.
- 비공개 게시글이면 `AUTH_TOKEN`을 설정한다.

## fixture 생성

`seed/fixture.sql`은 대상 게시글에만 `client_ip=benchmark-comment-pagination` 표식을 가진 댓글을 만든다. 댓글 수는 100, 1,000, 5,000부터 비교하고 필요할 때만 늘린다.

```sql
SET @benchmark_post_id = 100;
SET @benchmark_member_id = 1;
SET @benchmark_comment_count = 1000;
SET @benchmark_tree_shape = 'mixed';
SOURCE script/comment-pagination/seed/fixture.sql;
```

지원하는 트리 모양은 `flat`, `wide`, `mixed`다. 동일한 데이터 조건을 비교하려면 한 fixture를 만든 뒤 두 읽기 엔드포인트를 연속 측정한다.

정리할 때는 다음 변수를 지정한다.

```sql
SET @benchmark_post_id = 100;
SOURCE script/comment-pagination/seed/reset.sql;
```

## 실행

```bash
export POST_ID=100
export COMMENT_COUNT=1000
export TREE_SHAPE=mixed

script/comment-pagination/smoke.sh
script/comment-pagination/run.sh read v1
script/comment-pagination/run.sh read v2
script/comment-pagination/run.sh correctness
```

중간 또는 후반 cursor를 측정할 때는 준비 단계에서 넘길 페이지 수를 지정한다.

```bash
CURSOR_STEPS=10 script/comment-pagination/run.sh read v1
CURSOR_STEPS=10 script/comment-pagination/run.sh read v2
```

쓰기 guardrail은 인증이 필요하다. 개선 전 쓰기는 같은 배포에서 안전하게 재현할 수 없으므로, 현재 경로 저장 쓰기의 절대 지연을 루트와 답글로 나눠 남긴다.

```bash
AUTH_TOKEN='test-member-jwt' script/comment-pagination/run.sh write-root
AUTH_TOKEN='test-member-jwt' PARENT_COMMENT_ID=101 script/comment-pagination/run.sh write-reply
```

## 결과 파일

한 번 실행할 때 원본 두 개를 함께 남긴다.

- `results/raw/*-summary.json`: k6 summary와 p95·p99
- `results/raw/*-timeseries.csv`: k6 시계열

`EXPLAIN ANALYZE`와 Prometheus 값은 `results/metrics-TEMPLATE.csv` 형식으로 기록한다. 원본 실행 조건과 SQL 출력은 `results/TEMPLATE.md`에 함께 남긴다.

MySQL 출력을 저장한 뒤 비교 노드의 actual rows·loops와 정렬·구체화 여부를 CSV로 자동 추출할 수 있다.

```bash
python3 script/comment-pagination/collect_explain.py \
  --before script/comment-pagination/results/raw/before-explain.txt \
  --after script/comment-pagination/results/raw/after-explain.txt \
  --comments 1000 \
  --tree-shape mixed \
  --cursor-position first \
  --output script/comment-pagination/results/raw/2026-08-02-explain.csv
```

전체 JSON·CSV를 정규화하고 matplotlib 그래프를 생성한다.

```bash
script/measurements/run.sh
```

블로그에 사용하는 댓글 수별 그래프는 `comment-pagination-scale-latency`와 `comment-pagination-scale-rows`이며 범례는 `Before`, `After`로 표시된다.
