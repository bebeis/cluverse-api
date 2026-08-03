# 홈 최근 댓글 글 조회 측정 결과

## 실행 조건

- commit:
- environment:
- MySQL version:
- application instances:
- connection pool:
- 전체 댓글 수:
- 댓글이 있는 게시글 수:
- 최상위 게시글 댓글 집중도:
- rate / duration:

## 조회 결과

| 구분 | API p95 | API p99 | 실패율 | EXPLAIN actual rows | Loose Index Scan | sort / temporary |
|---|---:|---:|---:|---:|---|---|
| 개선 전 · 일반 GROUP BY | | | | | 미사용 | |
| 중간 · 인덱스 집계 + 캐시 | | | | | | |
| 개선 후 · 활동 투영 | | | | | 미사용 | |

## 중간 단계 캐시

| 지표 | 결과 | 확인 목적 |
|---|---:|---|
| 후보 스냅숏 적중률 | | 반복 집계 제거 여부 |
| 전체 집계 폴백 | | 후보 범위가 권한 분포에 충분한지 |

## 쓰기·정합성

| 지표 | 결과 | 기준 |
|---|---:|---:|
| 활동 메타 적용 후 댓글 작성 p95 | | |
| 활동 메타 적용 후 댓글 작성 p99 | | |
| 세 조회 응답 불일치 | | 0건 |
| 원본 댓글·활동 메타 불일치 | | 0건 |

## 원본과 캡처

- k6 개선 전 HTML / JSON / CSV:
- k6 중간 단계 HTML / JSON / CSV:
- k6 개선 후 HTML / JSON / CSV:
- 댓글 쓰기 HTML / JSON / CSV:
- EXPLAIN ANALYZE 개선 전:
- EXPLAIN ANALYZE 중간 단계:
- EXPLAIN ANALYZE 개선 후:
- 정합성 SQL:
- Grafana 절대 시간 범위:
- matplotlib PNG:
