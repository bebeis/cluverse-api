# 인기글 인라인 판정 최소 검증 결과

## 측정 환경

| 항목 | 값 |
|---|---|
| source | `2dd6f49` + working tree, ECR `sha256:efe1345f4090621c798ac814c8c6dd1c4c47cbe8a3aed20c3bd6231b72c9ba50` |
| 애플리케이션 | EC2 `t3.small`, 컨테이너 1,536 MiB, JVM heap 약 768 MiB |
| DB | MySQL 8.0 / EC2 `t3.small` |
| ECS task 수 | 2 |
| 부하 | 5 RPS, 60초, 조건별 3회 |
| fixture | 조건·API별 독립 게시글 1,000개 |

## 실제 API 지연

| API | inline off p95/p99 | inline on p95/p99 | p95/p99 관측 차이 | 실패율 / dropped |
|---|---:|---:|---:|---:|
| 좋아요 | 41.69/77.90 ms | 47.75/62.48 ms | +6.07/-15.42 ms | 0 / 0 |
| 댓글 | 57.28/97.55 ms | 61.06/75.10 ms | +3.78/-22.45 ms | 0 / 0 |

각 값은 반복 3회의 percentile 중앙값이다. p95에서는 작은 부가 비용이 관측됐지만 p99는 오히려
낮아졌다. 인라인 판정이 꼬리 지연을 개선한 것이 아니라, 5 RPS의 작은 표본에서 인프라 지터가
인라인 부가 비용보다 컸다고 해석한다. 따라서 이 결과로 p99 개선이나 정확한 p99 비용을 주장하지 않는다.

## DB 접근 범위

| SQL | key | actual rows | actual time | loops |
|---|---|---:|---:|---:|
| V2 post + like/comment snapshot | `post_id` | 1 | 0.000079..0.000135 ms | 1 |

MySQL은 세 테이블의 PK 단건 조인을 const access로 처리해 `Rows fetched before execution`으로
표시했다. 여기서 유효한 근거는 실행 시간이 아니라 `actual rows=1`, `loops=1`인 단건 접근 범위다.

## 정합성·실패 격리

| 확인 항목 | 결과 |
|---|---|
| 인라인 판정 off 시 processor 미호출 | 통과 |
| 인라인 판정 on 시 V2 호출 | 통과 |
| 판정 실패가 원 요청으로 전파되지 않음 | 통과 |
| 측정 경로의 중복 승격 | 해당 없음(고정 임계값) |

## devlog에 반영할 문장

> 점수 계산 자체는 단순하지만, 매 좋아요·댓글 요청에서 미리 집계한 카운트 snapshot을 읽는 DB 왕복이 하나 추가된다. 5 RPS로 60초씩 3회 측정한 중앙값에서 인라인 판정 연결 후 p95는 좋아요 `+6.07 ms`, 댓글 `+3.78 ms`였다. p99는 두 API 모두 증가하지 않아 이 표본만으로 정확한 꼬리 지연 비용을 분리할 수 없었다. snapshot 조회는 `post_id` 기준 `actual rows=1`이었고, 판정 실패는 원래 반응 요청으로 전파되지 않도록 격리했다.

## 해석

- p95 관측 차이는 좋아요 +6.07 ms, 댓글 +3.78 ms로 작았다.
- 총 3,606건에서 실패와 dropped iteration은 없었다.
- 측정 구간 20분의 Hikari pending 최댓값은 모든 태스크에서 0이었다.
- 같은 구간 MySQL running threads 최댓값은 3, CPU 최댓값은 약 5.08%였다.
- 측정 후 `popularity_inline_evaluation_enabled=true`로 복구했고 API health가 `UP`임을 확인했다.
