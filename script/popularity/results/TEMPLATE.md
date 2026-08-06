# 인기글 인라인 판정 최소 검증 결과

## 측정 환경

| 항목 | 값 |
|---|---|
| commit | |
| 애플리케이션 / DB 사양 | |
| ECS task 수 | |
| 부하 | 5 RPS, 60초, 조건별 3회 |
| fixture | 조건·API별 독립 게시글 1,000개 |

## 실제 API 지연

| API | inline off p95/p99 | inline on p95/p99 | p95/p99 관측 차이 | 실패율 / dropped |
|---|---:|---:|---:|---:|
| 좋아요 | | | | |
| 댓글 | | | | |

`python3 script/popularity/summarize_inline_results.py`의 출력을 사용한다.

## DB 접근 범위

| SQL | key | actual rows | actual time | loops |
|---|---|---:|---:|---:|
| V2 post + like/comment snapshot | | | | |

## 정합성·실패 격리

| 확인 항목 | 결과 |
|---|---|
| 인라인 판정 off 시 processor 미호출 | |
| 인라인 판정 on 시 V2 호출 | |
| 판정 실패가 원 요청으로 전파되지 않음 | |
| 측정 경로의 중복 승격 | 해당 없음(고정 임계값) |

## devlog에 반영할 문장

> 점수 계산 자체는 단순하지만, 매 좋아요·댓글 요청에서 미리 집계한 카운트 snapshot을 읽는 DB 왕복이 하나 추가된다. 실제 API 측정에서 인라인 판정 연결 후 좋아요 p95는 `X ms`, 댓글 p95는 `Y ms` 변했다. p99는 반복별 지터와 함께 기록하고, snapshot 조회는 `post_id` 단건 접근이었으며 판정 실패는 원래 반응 요청으로 전파되지 않도록 격리했다.

## 해석

- 증가량이 예상 지연 예산 안에 들어왔는가:
- Hikari pending, DB CPU 급증 없이 5 RPS를 유지했는가:
- 측정 후 `popularity_inline_evaluation_enabled=true`로 복구했는가:
