# 로컬맵 V1/V2 측정 결과

## 실행 조건

- 커밋:
- 환경 / 인스턴스:
- DB connection pool 크기:
- provider stub 지연:
- 시작·증가·최대 RPS:
- 반복 횟수 / 측정 시간:
- readiness: `STUB / experiment=true / stub=true`
- SLO: `쓰기 API p99 < 1000ms / 성공률 >= 99% / dropped = 0`
- V2 완료 게이트: `저장 iteration + 준비 호출 1건 == provider 관측 호출 수`

## 결과

| 지표 | V1 | V2 | 변화 |
|---|---:|---:|---:|
| 최대 SLO 통과 RPS |  |  |  |
| 쓰기 API p95 |  |  |  |
| 쓰기 API p99 |  |  |  |
| DB 트랜잭션 p95 |  |  |  |
| DB 트랜잭션 p99 |  |  |  |
| Hikari active 최대 |  |  |  |
| Hikari pending 최대 |  |  |  |
| 성공 처리량 |  |  |  |
| 실패율 |  |  |  |
| dropped iteration |  |  |  |
| provider 기대 / 관측 호출 | 해당 없음 |  |  |

## 정합성 게이트

- 변조 토큰 저장 성공: `0`
- 동일 requestId의 서로 다른 postId: `0`
- `(provider, source_fingerprint)` 중복: `0`
- 삭제·비공개 콘텐츠의 로컬맵 노출: `0`
- V2 비동기 provider 미도달: `0`

## 해석

- 관측 사실:
- 원인 추론:
- 채택 여부:
