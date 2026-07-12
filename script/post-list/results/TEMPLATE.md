# 게시글 목록 조회 V1~V4 성능 측정 결과

> 사용법: 이 파일을 `results/YYYY-MM-DD-<설명>.md` 로 복사한 뒤 값을 채운다.
> 예) `cp results/TEMPLATE.md results/2026-07-12-hotboard-1m.md`

## 측정 환경

| 항목 | 값 |
|------|-----|
| 측정 날짜 | YYYY-MM-DD |
| Git 커밋 | `<git rev-parse --short HEAD>` |
| 앱 인프라 사양 | ECS on EC2 t3.small ×N / (또는 로컬 <CPU/RAM>) |
| DB 인프라 사양 | MySQL EC2 t3.small / (또는 로컬) |
| 시드 규모 | board_id=2001001, post <100만 / 700만> 건 (05a / 05a+05b) |
| MySQL 버전 | 8.x.x |
| innodb_buffer_pool_size | <예: 1G> |
| 워밍업 여부 | <예: 동일 조건 1회 선행 실행 후 본 측정> |
| k6 버전 | v0.5x |
| BOARD_ID | 2001001 |
| SIZE | 20 |
| SORT / CATEGORY | LATEST / (없음) |

## 표 1 — profile 모드 (실사용 페이지 분포)

동일 트래픽 프로파일(traffic-profile 기본 세그먼트: 1p 50% / 2~10p 30% / 11~100p 15% / 101p~ 5%),
동일 RATE·DURATION 으로 버전만 교체해 측정.

| 버전 | RATE(req/s) | 실측 RPS | avg(ms) | p50 | p95 | p99 | max | 에러율 |
|------|-------------|----------|---------|-----|-----|-----|-----|--------|
| V1   |             |          |         |     |     |     |     |        |
| V2   |             |          |         |     |     |     |     |        |
| V3   |             |          |         |     |     |     |     |        |
| V4   |             |          |         |     |     |     |     |        |

- RATE: constant-arrival-rate 목표 도착률. 실측 RPS 는 k6 `http_reqs` 기준.
- V4 는 세션(진입+커서워크)이 iteration 단위이므로, 위 값은 **개별 요청** 기준
  (`cursor_entry_duration` + `cursor_step_duration` 통합 또는 `http_req_duration`)으로 기재.

## 표 2 — fixed 모드 (offset 구간 프로브)

`PAGE_MODE=fixed` 로 특정 page 만 반복 조회 (size=20 기준 offset 환산).

| 버전 | page 1 (offset 0) |  | page 5000 (offset 10만) |  | page 20000 (offset 40만) |  |
|------|------|------|------|------|------|------|
|      | avg  | p95/p99 | avg | p95/p99 | avg | p95/p99 |
| V1   |      |      |      |      |      |      |
| V2   |      |      |      |      |      |      |
| V3*  |      |      | —    | —    | —    | —    |
| V4** | —    | —    | —    | —    | —    | —    |

- \* V3 는 page 상한이 500 이라 page 5000/20000 을 호출할 수 없다(offset 10만/40만 N/A).
    V3 의 이득은 표 1(전체 COUNT 제거) 및 EXPLAIN(v3-count)로 본다.
- \*\* V4 는 **offset 개념이 없다.** 커서 이동은 깊이에 무관하게 비용이 일정하므로
    이 표 대신, 커서 스텝 깊이별 지연을 아래 표 2-b 로 기록한다.

### 표 2-b — V4 커서 이동 스텝 깊이별 지연 (offset 무관성 확인)

`post-list-cursor.k6.js` 의 `cursor_step_duration` 을 depth 태그별로 분해.
"깊은 스텝도 첫 스텝과 지연이 같다" = O(1) 을 보이는 게 목적.

| depth 구간 | avg(ms) | p95 | p99 | 샘플 수 |
|------------|---------|-----|-----|---------|
| d01        |         |     |     |         |
| d02-05     |         |     |     |         |
| d06-10     |         |     |     |         |
| d11-25     |         |     |     |         |
| d26-50     |         |     |     |         |
| d51+       |         |     |     |         |

## 표 3 — EXPLAIN 요약

`explain/*.sql` 각 쿼리의 EXPLAIN(ANALYZE) 핵심 지표.

| 쿼리 (파일) | type | key | rows (est) | actual rows | Extra |
|-------------|------|-----|-----------|-------------|-------|
| v1-list (offset 40만)     |  |  |  |  |  |
| v1-count                  |  |  |  |  |  |
| v2-ids (offset 40만)      |  |  |  |  | Using index? |
| v2-projection (IN 21개)   |  |  |  |  |  |
| v2-count                  |  |  |  |  | (= v1-count) |
| v3-count (cap 10001)      |  |  |  |  |  |
| v4-entry (날짜 앵커)      |  |  |  |  | Using index? |
| v4-next (커서)            |  |  |  |  |  |
| v4-prev (커서)            |  |  |  |  | Backward index scan? |

## 관찰 / 해석 (서술)

- V1 → V2: (deferred join 으로 깊은 offset 프로젝션 비용이 어떻게 줄었는가)
- V2 → V3: (전체 COUNT → 상한 COUNT 로 꼬리 지연/부하가 어떻게 줄었는가)
- V3 → V4: (offset 자체 제거로 깊이 무관 O(1) 이 되었는가, 트레이드오프는)
- 특이사항 / 재현 시 주의점:
