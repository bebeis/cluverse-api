# 댓글 페이지 조회 측정 결과

## 실행 조건

- commit:
- environment:
- MySQL version:
- application instances:
- connection pool:
- post ID:
- comment count:
- tree shape:
- cursor position:
- limit:
- rate / duration:

## 정합성

- 개선 전·후 댓글 ID와 순서 일치:
- 페이지 중복:
- path NULL:
- 부모 path 불일치:

## API

| 구분 | 댓글 API p95 | 댓글 API p99 | 화면 완료 p95 | 화면 완료 p99 | 실패율 |
|---|---:|---:|---:|---:|---:|
| 개선 전 | | | | | |
| 개선 후 | | | | | |

## DB

| 구분 | actual rows | loops | temporary/filesort | Hikari pending max |
|---|---:|---:|---|---:|
| 개선 전 | | | | |
| 개선 후 | | | | |

## 쓰기 guardrail

| 구분 | p95 | p99 |
|---|---:|---:|
| 루트 댓글 | | |
| 답글 | | |

## 원본

- k6 summary JSON:
- k6 timeseries CSV:
- EXPLAIN ANALYZE:
- Prometheus CSV:
