# 홈 최근 댓글 글 조회 3단계 측정

같은 데이터와 응답 계약에서 세 구조를 비교한다. API 경로는 한 배포에서 동시에 측정하기 위해 `v1`, `v2`, `v3`로 나누고 결과 표와 그래프에는 `Before`, `Intermediate`, `After`로 표시한다.

1. 일반 `GROUP BY`: 활성 댓글 전체를 게시글별로 집계한다.
2. Loose Index Scan + 캐시: STORED generated column과 `(post_id, visible_created_at)` 인덱스로 그룹 대표값만 읽고, 전역 후보 스냅숏을 1분간 캐시한다. 게시글 접근 권한은 요청마다 다시 확인한다.
3. 활동 투영: 댓글 쓰기 시 `post_comment_activity`를 갱신하고 최신순 인덱스의 앞부분만 읽는다.

## 측정 지표

- 최근 댓글 글 API p95·p99
- `EXPLAIN ANALYZE`의 actual rows, Loose Index Scan·sort·temporary 여부
- 중간 단계의 캐시 적중률과 접근 가능한 후보가 부족할 때의 폴백 횟수
- 활동 메타 적용 후 댓글 작성 p95·p99
- 세 API 응답 불일치와 원본 댓글·활동 메타 불일치 건수

응답이 10건이라는 사실만으로 DB 방문 범위가 제한되는지 확인하는 데 필요한 지표만 남긴다. 처리량과 실패율은 부하가 정상적으로 유지됐는지 판단하는 보조값으로 함께 기록한다.

## API

- 개선 전: `GET /api/v1/home/recent-commented-posts`
- 중간 개선: `GET /api/v2/home/recent-commented-posts`
- 개선 후: `GET /api/v3/home/recent-commented-posts`
- 쓰기 guardrail: `POST /api/v1/comments?postId={postId}`

홈의 다른 컴포넌트는 이 실험과 별개다. 장소 추천은 `/api/v2/local-maps/...`, 실시간 인기글은 `/api/v2/popular-posts/recent?size=10`을 그대로 사용한다.

## fixture

전용 DB에서 활성 회원과 공개 게시판 ID를 지정한다. 시드는 제목·IP 표식이 있는 게시글을 새로 만들므로 프로덕션 DB에서는 실행하지 않는다. 생성된 게시글 ID는 삽입 후 표식과 ordinal로 다시 조회하므로 auto increment의 연속성에 의존하지 않는다.

```sql
SET @benchmark_member_id = 1;
SET @benchmark_board_id = 1;
SET @benchmark_post_count = 1000;
SET @benchmark_comment_count = 100000;
SET @benchmark_hot_comment_percent = 20;
SOURCE script/home-feed/seed/fixture.sql;
```

`hot_comment_percent=20`은 전체 댓글의 20%를 한 게시글에 몰아 실제 커뮤니티의 편향을 단순화해 재현한다. 규모 비교는 먼저 10만, 100만 순서로 올리고 DB 여유를 확인한 뒤 확장한다.

정리는 다음 스크립트로 수행한다.

```sql
SOURCE script/home-feed/seed/reset.sql;
```

## 세션 준비

Cluverse 인증은 Bearer 토큰이 아니라 HTTP 세션을 사용한다. 브라우저 개발자 도구나 로그인 응답의 `Set-Cookie`에서 세션을 가져와 다음처럼 지정한다.

```bash
export SESSION_COOKIE='JSESSIONID=replace-me'
export BASE_URL='http://localhost:8080'
```

비밀값은 `k6 -e` 인자로 넘기지 않고 프로세스 환경으로만 전달한다.

## 실행

같은 fixture에서 쓰기 부하 없이 세 단계를 연속 실행한다. 중간 단계는 `setup()`에서 한 번 호출해 후보 캐시를 예열하므로 단일 애플리케이션 인스턴스의 커스텀 지연 지표에는 warm-cache 요청만 포함된다. Caffeine은 인스턴스별 캐시이므로 여러 인스턴스를 측정할 때는 각 인스턴스를 직접 예열하거나 cold miss가 포함됐음을 실행 조건에 기록한다.

```bash
export COMMENTS=100000
export COMMENTED_POSTS=1000
export HOT_COMMENT_PERCENT=20

script/home-feed/smoke.sh
script/home-feed/run.sh read v1
script/home-feed/run.sh read v2
script/home-feed/run.sh read v3
script/home-feed/run.sh correctness
```

정합성 비교는 고정 fixture에서만 실행한다. 세 조회 사이에 댓글이 생성·삭제되면 서로 다른 스냅숏을 읽으므로 알고리즘 동등성 검증이 아니다.

쓰기 비용은 fixture 게시글 하나를 지정해 낮은 rate부터 확인한다.

```bash
POST_ID=123 RATE=2 DURATION=30s script/home-feed/run.sh write
```

쓰기 실행 후에는 `verify-integrity.sql`로 투영 정합성을 확인한다. 실행이 만든 댓글까지 지우려면 fixture 전체를 reset하거나 출력된 `run_id`의 `home-feed-benchmark:{run_id}` 댓글을 별도로 정리한다.

## EXPLAIN ANALYZE

```bash
mysql -h127.0.0.1 -ucluverse_user -p cluverse_v2 \
  < script/home-feed/explain/v1-group-aggregate.sql \
  | tee script/home-feed/results/raw/v1-explain.txt

mysql -h127.0.0.1 -ucluverse_user -p cluverse_v2 \
  < script/home-feed/explain/v2-loose-index-snapshot.sql \
  | tee script/home-feed/results/raw/v2-explain.txt

mysql -h127.0.0.1 -ucluverse_user -p cluverse_v2 \
  < script/home-feed/explain/v3-activity-range.sql \
  | tee script/home-feed/results/raw/v3-explain.txt
```

출력을 공통 CSV로 변환한다.

중간 단계의 EXPLAIN은 1분마다 다시 계산되는 후보 스냅숏의 cold refresh 비용이다. k6의 warm-cache API 지연과 같은 값으로 해석하지 않는다.

```bash
python3 script/home-feed/collect_explain.py \
  --v1 script/home-feed/results/raw/v1-explain.txt \
  --v2 script/home-feed/results/raw/v2-explain.txt \
  --v3 script/home-feed/results/raw/v3-explain.txt \
  --comments 100000 \
  --commented-posts 1000 \
  --hot-comment-percent 20 \
  --output script/home-feed/results/raw/2026-08-03-explain.csv
```

## 캡처 산출물

`run.sh` 한 번에 다음 네 파일을 남긴다.

- `results/raw/*.html`: p95·p99와 실패율을 바로 캡처하는 k6 웹 리포트
- `results/raw/*-console.txt`: 버전, 데이터 규모, 집중도, rate·duration 조건
- `results/raw/*-summary.json`: 재계산 가능한 k6 요약
- `results/raw/*-timeseries.csv`: 시계열 원본

Grafana 대시보드는 같은 절대 시간 범위에서 세 조회의 p95·p99, 댓글 쓰기 p95·p99, 중간 단계의 캐시 적중률·폴백 빈도를 캡처한다. JSON·CSV를 합쳐 matplotlib 그래프를 만들 때는 기존 실험과 함께 다음을 실행한다.

```bash
script/measurements/run.sh
```

홈 피드용 핵심 이미지는 `home-feed-scale-latency.png`와 `home-feed-scale-rows.png`다.
