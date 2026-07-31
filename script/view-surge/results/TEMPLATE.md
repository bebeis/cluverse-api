# 게시글 급상승 감지 + Redis Write-back(V4) 성능 측정 결과

> 사용법: 이 파일을 `results/YYYY-MM-DD-<설명>.md` 로 복사한 뒤 값을 채운다.
> 예) `cp results/TEMPLATE.md results/2026-08-01-surge.md`

## 측정 환경

| 항목 | 값 |
|------|-----|
| 측정 날짜 | YYYY-MM-DD |
| Git 커밋 | `<git rev-parse --short HEAD>` |
| 앱 인프라 사양 | ECS on EC2 t3.small ×N / (또는 로컬 <CPU/RAM>) |
| DB 인프라 사양 | MySQL EC2 t3.small / (또는 로컬) |
| Redis 인프라 사양 | Redis EC2 t3.micro (10.0.11.20) / (또는 로컬 컨테이너) |
| 시드 규모 | post 약 3천만 건 (05+05a+05b+05d, post_id 상한 30000000 / 실 행수 약 2700만) |
| MySQL 버전 | 8.x.x |
| Redis 버전 | 6.x / 7.x |
| innodb_buffer_pool_size | <예: 1G> |
| HikariCP maximumPoolSize | <예: 10> |
| view-surge.window / threshold | `10s` / `200` |
| view-surge.sustain-threshold | `100` (flush 델타가 이 값 이상이면 만료 연장) |
| view-surge.tracking-ttl / extension / grace | `5m` / `5m` / `15s` |
| flush / 라우팅 갱신 / 정리 주기 | `3s` / `3s` / `10s` |
| 워밍업 여부 | <예: 동일 조건 1회 선행 실행 후 본 측정> |
| k6 버전 | v0.5x |
| HOT_POST_ID | 5999999 |
| POST_ID_MIN ~ MAX (배경 분포) | 5000001 ~ 13999999 |

## 표 1 — V3 계단식 부하 (단일 게시글, 임계값 도출)

`EXECUTOR=steps -e POST_MODE=fixed` 로 게시글 하나에 도착률을 계단으로 올린다.
**어느 계단부터 무너지는가**가 곧 급상승 감지 임계값의 근거다.

```bash
script/view-surge/run.sh bench -e VERSION=v3 -e EXECUTOR=steps \
       -e STEP_RATES=50,100,150,200,250,300 -e STEP_DURATION=1m
```

| step rate(req/s) | 실측 RPS | p50(ms) | p99(ms) | max(ms) | 에러율 | 락 대기 스냅샷* |
|------------------|----------|---------|---------|---------|--------|-----------------|
| 50               |          |         |         |         |        |                 |
| 100              |          |         |         |         |        |                 |
| 150              |          |         |         |         |        |                 |
| 200              |          |         |         |         |        |                 |
| 250              |          |         |         |         |        |                 |
| 300              |          |         |         |         |        |                 |

- 계단별 지연은 k6 summary 의 `view_count_duration{step:rNNN}` sub-metric 에서 읽는다.
- 실측 RPS 는 계단 경계를 시간으로 잘라 HTML 리포트 시계열에서 읽는다
  (`http_reqs` 총계는 전 계단 합산이라 계단별로는 쓸 수 없다).
- \* 각 계단이 도는 동안 별도 세션에서 `lock-waits.sql` 1~3절 실행 → 대기 행 수 기록.
- **판정**: p99 가 꺾이고 에러율이 오르기 시작한 계단 = V3 한계선 → `______ req/s`.

## 표 2 — V3 vs V4 상시 오버헤드 (균등 분포)

급상승이 아닌 평상시 트래픽. 쓰기가 흩어져 있으므로 두 버전의 차이는
**감지 로직(속도 집계)의 비용**만 남는다.

| 버전 | 분포 | RATE(req/s) | 실측 RPS | p50(ms) | p99(ms) | 에러율 |
|------|------|-------------|----------|---------|---------|--------|
| V3 | profile |        |          |         |         |        |
| V4 | profile |        |          |         |         |        |
| V3 | zipf    |        |          |         |         |        |
| V4 | zipf    |        |          |         |         |        |

- 오버헤드 = (V4 p50 - V3 p50) / V3 p50 = `____ %`
- 이 구간에서 `view_surge_activation_total` 이 증가하면 배경 트래픽이 감지를
  유발한 것이다 — 임계값이 낮거나 분포가 지나치게 쏠려 있다는 신호.

## 표 3 — 라이프사이클 (감지 → 전환 → 복귀)

```bash
script/view-surge/run.sh lifecycle -e HOT_POST_ID=5999999 -e TAIL=8m
```

| 실행 파라미터 | 값 |
|---------------|-----|
| RAMP_TARGET / RAMP_UP | 300 / 30s |
| SUSTAIN / DECAY_TO / DECAY / COOL / TAIL | 3m / 5 / 1m / 2m / 8m |
| BG_RATE / BG_MODE | 50 / profile |
| 램프 시작 시각 (t0) |  |

### 3-1. 구간별 hotspot 지연

| 구간 | 경과 시간대 | 도착률(req/s) | p50(ms) | p99(ms) | 비고 |
|------|-------------|---------------|---------|---------|------|
| 램프업 (RAMP_UP) |  | 5 → 300 |  |  | 감지 직전까지 MySQL 경로 |
| 유지 - 전환 전 |  | 300 |  |  | 지연 최고점 예상 구간 |
| 유지 - 전환 후 |  | 300 |  |  | Redis 경로. p99 가 떨어져야 함 |
| 감쇠 (DECAY) |  | 300 → 5 |  |  |  |
| 저트래픽 유지 (COOL) |  | 5 |  |  | TTL 경과 대기 |
| 정리 후 (TAIL, 배경만) |  | - |  |  | MySQL 경로 복귀 |

- 구간별 값은 k6 HTML 리포트(`results/raw/*.html`)의 `hot_duration` 시계열을
  시간대로 잘라 읽는다. summary 의 단일 p99 는 전 구간 혼합이라 쓰지 않는다.
- 배경 트래픽 지연은 `http_req_duration{scenario:background}` 로 함께 확인한다
  — hotspot 경합이 배경까지 끌고 들어갔는지 판단하는 대조군이다.

### 3-2. 전이 시점

| 이벤트 | 시각 | 램프 시작 기준 경과 | 근거 |
|--------|------|---------------------|------|
| 램프 시작 (t0) |  | 0s | k6 실행 시각 |
| 급상승 감지 (activated_at) |  |  | `view_surge_tracking.activated_at` / `view_surge_activation_total` 증가 |
| Redis 경로 전환 완료 |  |  | `view_count_redis_path_total` 이 요청량과 같은 기울기로 증가 시작 |
| 지연 하락 관측 |  |  | k6 `hot_duration` p99 하락 시점 |
| 만료 연장 발생 횟수 |  | - | `view_surge_extension_total` 델타 = ____ 회 |
| 정리 완료 (복귀) |  |  | `view_surge_cleanup_total` 증가 + Redis 키/추적 행 삭제 확인 |

- **감지 지연** = (activated_at - 램프 시작) = `____ s`
- **전환 지연** = (Redis 경로 전환 완료 - activated_at) = `____ s` (라우팅 캐시 갱신 주기 3초 이내여야 정상)
- 세 근거(Grafana 메트릭 / `view_surge_tracking` 타임스탬프 / k6 p99 시계열)가
  서로 어긋나면 어긋난 사실 자체를 적는다 — 그 간극이 곧 관찰 가능성의 한계다.

### 3-3. flush 통계

| 항목 | 값 |
|------|-----|
| `view_surge_flush_batch_size` 평균 / 최대 |  |
| `view_surge_flush_duration_seconds` p99 / max |  |
| `view_surge_routing_cache_size` 최대 |  |
| `view_surge_sample_cache_size` 최대 |  |
| `view_surge_flush_restored_total` |  |
| MySQL 쓰기 감소율* |  |

- \* (요청 수 - 실제 UPDATE 수) / 요청 수. 접힘 비율이 V4 의 핵심 이득이다.

## 표 4 — 역주행 (오래된 글의 급상승)

`HOT_POST_ID` 를 일반 200만 시드 구간(3000001~5000000)의 오래된 글로 바꿔 실행.
버퍼 풀에 없는 레코드라 MySQL 경로의 초기 지연이 더 크고, 전환 이득도 커야 한다.

| 항목 | 최신글 (표 3) | 역주행 (오래된 글) | 차이 |
|------|---------------|--------------------|------|
| 전환 전 p99(ms) |  |  |  |
| 전환 후 p99(ms) |  |  |  |
| 감지 지연(s) |  |  |  |
| 전환 지연(s) |  |  |  |

- 두 경우의 감지·전환 지연이 같다면, 감지 로직이 레코드의 물리적 위치와
  무관하게 동작한다는 뜻이다(의도한 동작).

## 표 5 — Redis 장애 fallback

급상승 전환이 끝난 뒤 Redis 를 내리고, 앱이 MySQL 경로로 떨어지는지 확인한다.

```bash
# bastion 에서
sudo systemctl stop redis6      # 장애 주입
sudo systemctl start redis6     # 복구
```

| 단계 | 시각 | hotspot p99(ms) | 에러율 | `view_count_redis_fallback_total{origin=request/flush}` | 비고 |
|------|------|------------------|--------|-----------------------------------|------|
| 정상 (Redis 경로) |  |  |  |  |  |
| Redis 중단 직후 |  |  |  |  | 요청이 실패하지 않고 MySQL 로 떨어지는가 |
| 중단 지속 중 |  |  |  |  | V3 수준 지연으로 수렴하는가 |
| Redis 복구 직후 |  |  |  |  | 다시 Redis 경로로 돌아오는가 |

- **판정 기준**: 중단 구간에서 에러율이 오르지 않아야 한다(성능 저하는 허용, 실패는 불가).
- 중단 시점에 Redis 에 남아 있던 미flush 증가분은 유실된다 — 유실량을 표 6에 기록한다.

## 표 6 — 정합성 검증

`explain/verify-integrity.sql` + `redis-cli` 로 확인. 부하를 멈추고 정리가 끝난
**최종 상태**에서 판정한다.

| 시나리오 | k6 성공 요청 수 | MySQL 증가분 | Redis 잔여분 | 합계 일치 | 비고 |
|----------|-----------------|--------------|--------------|-----------|------|
| 라이프사이클 (표 3) |  |  |  | O / X |  |
| 역주행 (표 4) |  |  |  | O / X |  |
| Redis 장애 (표 5) |  |  |  | O / X | 유실량 = ____ 건 |

- 최종 상태에서 Redis 잔여분은 0, 추적 행도 삭제돼 있어야 한다.

## 표 7 — EXPLAIN 요약

| 쿼리 (파일) | type | key | rows | Extra |
|-------------|------|-----|------|-------|
| v4-increase-and-get UPDATE |  |  |  |  |
| v4-surge-upsert INSERT..ODKU [A] |  |  |  |  |
| v4-surge-upsert 연장 UPDATE [B] |  |  |  |  |
| v4-tracking-scan 활성 [A] |  |  |  |  |
| v4-tracking-scan 정리 [B] |  |  |  |  |
| v4-flush-update UPDATE |  |  |  |  |

- 조회수 UPDATE 3종은 모두 PK 단건이어야 한다 — V3 와 플랜이 같다.
- 추적 스캔 2종은 `idx_expires_at` 을 타야 한다. filesort 가 뜨면 기록.

## 관찰 / 해석 (서술)

- V3 한계선: (어느 계단에서 무너졌는가, 무너지는 양상은 지연인가 에러인가)
- V4 상시 오버헤드: (평상시 트래픽에서 감지 로직이 얼마를 먹는가, 감수할 만한가)
- 감지 → 전환: (임계값을 넘고 실제로 Redis 경로가 되기까지 몇 초가 걸렸는가,
  그 사이 사용자가 겪은 지연은)
- 전환 효과: (같은 도착률에서 p99 가 얼마나 떨어졌는가, MySQL 쓰기는 몇 배로 접혔는가)
- 복귀: (열기가 식은 뒤 제때 정리됐는가, 정리 누락·고아 키는 없었는가)
- 역주행: (오래된 글에서 달랐던 점)
- Redis 장애: (fallback 이 실제로 동작했는가, 유실 범위는 어디까지인가)
- 특이사항 / 재현 시 주의점:
