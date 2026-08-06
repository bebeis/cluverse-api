# Devlog 6 인기글 인라인 판정 최소 검증

이 devlog에서는 V1 전체 배치와 V2 단건 API의 처리량을 직접 비교하지 않는다. 두 요청은
처리 단위가 다르고, V1은 스케줄 배치이며 V2는 실제 좋아요·댓글 요청에 동기로 연결되기 때문이다.

본문에는 다음 근거만 사용한다.

1. 인라인 판정 연결 전·후의 실제 좋아요 API p95/p99
2. 인라인 판정 연결 전·후의 실제 댓글 API p95/p99
3. V2 snapshot SQL의 `EXPLAIN ANALYZE actual rows`
4. 승격 판정 실패가 원래 요청을 실패시키지 않는 테스트

최대 TPS, V1 배치 p99, 게시판별 승격률 편차는 이 글의 성능 결과로 사용하지 않는다.

## 측정 토글

운영 기본값은 인라인 판정 활성화다.

```yaml
popularity:
  inline-evaluation:
    enabled: true
```

AWS 측정 배포에서는 Terraform 변수로 좋아요·댓글 저장만 실행하는 기준선과, 저장 후
V2 판정까지 실행하는 조건을 만든다.

```bash
# 기준선: 좋아요·댓글 저장만 수행
terraform -chdir=terraform/test apply \
  -var='popularity_inline_evaluation_enabled=false'

# 개선 경로: 저장 후 V2 판정 수행
terraform -chdir=terraform/test apply \
  -var='popularity_inline_evaluation_enabled=true'
```

측정이 끝난 후에는 반드시 `true`로 복구한다.

## 1. 새 이미지 배포

```bash
export AWS_PROFILE=cluverse-terraform
export BASE_URL=https://api.cluverse.cona.team

script/aws/push-image.sh latest

aws ecs wait services-stable \
  --cluster cluverse-test \
  --services cluverse-api \
  --region ap-northeast-2
```

## 2. 동일 조건 fixture 적재

```bash
mysql -h127.0.0.1 -ucluverse_user -p cluverse_v2 \
  < script/popularity/seed/inline-overhead-fixture.sql
```

fixture는 다음 네 그룹에 각각 1,000개의 게시글을 만든다.

| 조건 | 좋아요 | 댓글 |
|---|---:|---:|
| inline off | `9200000001~9200001000` | `9200002001~9200003000` |
| inline on | `9200001001~9200002000` | `9200003001~9200004000` |

게시판 임계값은 1,000,000으로 고정해 여기서는 `popular_post` UPSERT 비용을 섞지 않고, 매 반응의
snapshot 조회와 판정 부가 비용만 측정한다. 승격 UPSERT와 멱등성은 기존 `seed/fixture.sql`과
단위 테스트로 분리한다.

## 3. 기준선: inline off

```bash
terraform -chdir=terraform/test apply \
  -var='popularity_inline_evaluation_enabled=false'

aws ecs wait services-stable \
  --cluster cluverse-test \
  --services cluverse-api \
  --region ap-northeast-2
```

조건별 5 RPS, 60초, 3회를 실행한다. `run-inline.sh`는 301번째 경계 요청까지 고려해
반복 구간을 320개씩 이동하므로 게시글이 겹치지 않는다.

```bash
for KIND in like comment; do
  for REPEAT in 1 2 3; do
    BASE_URL="$BASE_URL" CONDITION=disabled KIND="$KIND" REPEAT="$REPEAT" \
      script/popularity/run-inline.sh
  done
done
```

## 4. V2 연결: inline on

```bash
terraform -chdir=terraform/test apply \
  -var='popularity_inline_evaluation_enabled=true'

aws ecs wait services-stable \
  --cluster cluverse-test \
  --services cluverse-api \
  --region ap-northeast-2
```

```bash
for KIND in like comment; do
  for REPEAT in 1 2 3; do
    BASE_URL="$BASE_URL" CONDITION=enabled KIND="$KIND" REPEAT="$REPEAT" \
      script/popularity/run-inline.sh
  done
done
```

각 실행은 `results/raw/`에 summary JSON·HTML을 남기고 `results/inline-metrics.csv`에 한 행을 추가한다.
조건별 3회 p95/p99의 중앙값과 각 관측 차이는 다음 명령으로 출력한다.

```bash
python3 script/popularity/summarize_inline_results.py
```

실패율이 0이 아니거나 `dropped_iterations`가 0이 아니면 결과를 폐기한다.

## 5. 단건 조회 근거

```bash
mysql -h127.0.0.1 -ucluverse_user -p cluverse_v2 \
  < script/popularity/explain/v2-post-snapshot.sql \
  > script/popularity/results/v2-post-snapshot-explain.txt
```

`EXPLAIN ANALYZE`에서 `actual rows`, `actual time`, `loops`를 기록한다. 핵심은 매 반응마다
`COUNT(*)`를 실행하는 것이 아니라, `post_id` 단건으로 게시글·좋아요 카운트·댓글 카운트를 읽는다는
것을 확인하는 데 있다.

## 6. 실패 격리 검증

```bash
./gradlew test \
  --tests 'cluverse.popularity.service.implement.PopularityPromotionInvokerTest'
```

인기글 판정 트랜잭션이 실패해도 예외를 원래 좋아요·댓글 요청으로 전파하지 않는지 확인한다.

## 7. 결과 기록과 해석

```bash
cp script/popularity/results/TEMPLATE.md \
  script/popularity/results/$(date +%F)-popularity-inline.md
```

본문에서는 "성능이 N배 개선됐다"가 아니라 다음만 주장한다.

- 점수 계산은 단순하지만 매 반응에 snapshot DB 조회가 하나 추가된다.
- 실제 좋아요·댓글 API의 p95/p99 관측 차이를 함께 기록했다.
- p99 변동 방향이 p95와 다르면 인라인 로직의 개선으로 해석하지 않고 측정 지터를 명시했다.
- 조회는 미리 집계한 카운트 테이블을 `post_id`로 읽으며, 실제 접근 범위는 단건이었다.
- 판정 실패는 원래 반응 요청의 실패로 전파되지 않도록 격리했다.

fixture 제거가 필요하면 다음을 실행한다.

```bash
mysql -h127.0.0.1 -ucluverse_user -p cluverse_v2 \
  < script/popularity/seed/inline-overhead-reset.sql
```
