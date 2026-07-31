# 게시글 급상승 감지 + Redis Write-back(V4) 성능 측정 도구

조회수 증가 API가 **단일 게시글에 트래픽이 몰리는 상황**을 어떻게 견디는지 측정하기 위한
k6 부하 스크립트 + EXPLAIN/락 관찰 SQL 모음입니다. 기존 V3(원자적 UPDATE)를 기준선으로
두고, 급상승 감지 후 Redis 로 쓰기를 우회시키는 V4를 같은 조건으로 비교합니다.

| 버전 | 엔드포인트 | 쓰기 경로 | 비고 |
|------|-----------|-----------|------|
| V3 | `POST /api/v3/posts/{postId}/view-count` | 항상 MySQL 원자적 UPDATE | 기준선 (조회수 편에서 측정 완료) |
| V4 | `POST /api/v4/posts/{postId}/view-count` | 평상시 MySQL / 급상승 감지 시 Redis 누적 → 주기 flush | 신규 |

```
script/view-surge/
  run.sh                          ← k6 실행 래퍼 (웹 대시보드 + HTML 리포트 자동 저장)
  k6/
    lib/traffic-profile.js        ← 배경 트래픽 분포 (세그먼트 + Zipf, 수치 단일 출처)
    view-surge-bench.k6.js        ← V3/V4 공용 벤치 (fixed/profile/zipf × constant/steps)
    view-surge-lifecycle.k6.js    ← V4 급상승 생애주기 (배경 + 급상승 2 시나리오 동시)
  explain/
    v4-increase-and-get.sql       ← 평상시 경로 UPDATE + LAST_INSERT_ID 회수
    v4-surge-upsert.sql           ← 감지 시 추적 테이블 UPSERT
    v4-tracking-scan.sql          ← 라우팅 캐시 갱신 / 만료 스캔
    v4-flush-update.sql           ← Redis 누적분 되쓰기 UPDATE
    lock-waits.sql                ← 부하 중 락 경합 관찰 스냅샷
    verify-integrity.sql          ← MySQL + Redis 합산 정합성 검증
  results/TEMPLATE.md             ← 측정 결과 기록 양식
```

본 측정에서는 `k6 run ... k6/<스크립트>` 대신 `run.sh bench|lifecycle ...` 를 권장합니다.
동일한 k6 인자를 받되, 실시간 웹 대시보드(http://localhost:5665)와
`results/raw/<날짜시각>-<라벨>.html` 리포트 저장, VU 풀 기본값(100/600)을 자동으로 켭니다.

```bash
script/view-surge/run.sh bench -e VERSION=v3 -e RATE=300 -e DURATION=1m
```

**측정의 핵심**: V4의 쿼리는 V3와 다르지 않습니다 — 조회수 UPDATE 3종 모두 PK 단건 접근입니다.
차이는 **같은 레코드에 쓰기가 얼마나 자주 도달하는가**입니다. V3는 요청당 1회,
V4는 전환 후 게시글당 flush 주기(3초)당 1회. 그래서 이 도구의 목적은 두 가지입니다.

1. V3가 무너지는 지점을 계단 부하로 찾아, 감지 임계값의 근거를 만든다.
2. 급상승 게시글 하나의 생애주기(감지 → 전환 → 복귀)에서 지연이 어떻게 변하는지,
   그리고 그 전이가 **몇 초 만에** 일어나는지를 관찰한다.

두 번째가 이 편의 진짜 주제입니다. 정상 상태의 처리량 비교는 표 2 한 줄이면 끝나고,
나머지는 전부 **상태 전이 구간**을 보기 위한 장치입니다.

---

## 빠른 시작 (로컬에서 10분 스모크)

측정 파이프라인과 급상승 전환이 실제로 동작하는지 로컬에서 먼저 확인합니다.
본 측정은 아래 [본 측정 절차](#본-측정-절차)로.

**딸깍 검증**: 아래 수동 절차를 자동화한 e2e 스크립트가 있습니다 — 라이프사이클
(감지→전환→플러시→연장→정리→복귀)과 정합성(성공 요청 수 == view_count 증가량),
Redis 장애 폴백까지 단계별로 assert하고 실패 시 exit 1.

```bash
docker compose up -d          # 스택 기동 (mysql 볼륨에 V1 baseline 적용돼 있어야 함)
script/view-surge/smoke.sh                # 전체 (Redis stop/start 포함, 약 3분)
script/view-surge/smoke.sh --skip-outage  # 장애 주입 생략 (약 90초)
```

재실행 안전(멱등)하며 전용 post_id(900000001)만 사용합니다. V4 코드 변경 후
회귀 확인은 `./gradlew test` + 이 스크립트 한 번이면 됩니다.

```bash
# 0) k6 설치 (v0.50+ 필요)
brew install k6

# 1) MySQL + Redis 기동 (리포 루트의 docker-compose)
docker compose up -d db redis

# 2) 앱 기동 — 감지 임계값을 낮춰서 손으로도 급상승을 만들 수 있게 한다
#    (기본값 threshold=200/window=10s 는 로컬 curl 루프로 넘기기 어렵다)
#    TTL 도 함께 낮춰야 정리(복귀)까지 몇 분 안에 볼 수 있다
./gradlew bootRun --args='--view-surge.threshold=30 --view-surge.window=5s \
  --view-surge.sustain-threshold=10 --view-surge.tracking-ttl=30s --view-surge.grace=5s'

# 3) 시드 적재 (05a까지면 스모크에 충분)
cd docs/v1/ddl/test-data
mysql -h127.0.0.1 -ucluverse_user -ptest1234 cluverse_v2 < 01_university_seed.sql
# ... 02, 03, 04, 05, 05a 순서대로

# 4) 급상승 유발 — 한 게시글에 5초 안에 30건 이상 때린다
for i in $(seq 1 100); do
  curl -s -X POST http://localhost:8080/api/v4/posts/5999999/view-count > /dev/null
done
```

전환이 일어났다면 아래 세 곳에서 같은 사실이 보입니다.

```bash
# (a) 추적 테이블에 등록됐는가 (activated_at = 감지 시각)
mysql -h127.0.0.1 -ucluverse_user -ptest1234 cluverse_v2 \
      -e "SELECT post_id, activated_at, expires_at FROM view_surge_tracking;"

# (b) Redis 에 증가분이 쌓이는가 (라우팅 캐시 갱신 ~3초 후부터)
#     flush 는 키를 0 으로 초기화만 하므로, 전환 후에는 "0" 또는 작은 값이 정상
redis-cli -h 127.0.0.1 GET view:pending:5999999

# (c) 메트릭이 올라갔는가
curl -s http://localhost:8080/actuator/prometheus | grep -E 'view_surge_|view_count_redis_'
```

이어서 k6 스크립트가 도는지도 짧게 확인합니다.

```bash
script/view-surge/run.sh bench -e VERSION=v4 -e RATE=5 -e DURATION=10s
script/view-surge/run.sh lifecycle -e RAMP_UP=10s -e SUSTAIN=30s \
       -e DECAY=10s -e COOL=30s -e TAIL=30s -e RAMP_TARGET=50
```

`view_count_success_rate` 가 100%에 가깝고 `hot_duration` 에 값이 찍히면 성공입니다.
부하를 멈추고 30초쯤 뒤 (a)의 추적 행과 (b)의 Redis 키가 **사라져 있으면** 정리까지 정상입니다.

---

## 본 측정 절차

측정은 6단계입니다. 각 단계의 결과를 `results/` 에 바로 적으면서 진행하는 걸 권장합니다.
전체 소요는 대기 시간 포함 두 시간 남짓입니다.

### Step 0. 준비

```bash
script/aws/up.sh --seed view-count --8m --30m   # 인프라 기동 + 시드 (post_id 상한 3,000만)
script/aws/tunnel.sh start                      # Grafana http://localhost:3000
```

- [ ] 시드 적재 완료 — `--8m`(핫보드 700만)과 `--30m`(일반 게시판 1,600만)은 서로 독립이며
      둘 다 넣으면 실 행수 2,700만입니다. **매우 오래 걸립니다**(05d 만 수 시간 + 디스크 9GB).
      시딩은 bastion 에서 nohup 으로 돌아가므로 `script/aws/seed.sh --follow` 로 확인합니다.
      규모를 줄여 측정했다면 결과 파일의 시드 규모 행을 실제 값으로 고칩니다.
- [ ] 통계 갱신 — 대량 적재 직후에는 옵티마이저 통계가 낡아 EXPLAIN 의 rows 추정이 엉뚱합니다.
      ```bash
      mysql ... -e "ANALYZE TABLE post_view_count, view_surge_tracking;"
      ```
- [ ] 앱 기동 확인 — `curl -X POST "$BASE_URL/api/v4/posts/5999999/view-count"` 가 `"code":200`
      (view-count 계열은 부하테스트용으로 비로그인 허용)
- [ ] 서버 설정 확인 — `application.yml` 의 `view-surge:` 블록.
      **본 측정은 기본값으로 합니다.** 로컬 스모크에서 낮춰 둔 값이 배포에 섞이지 않았는지 확인.

  | 설정 | 기본값 | 의미 |
  |------|--------|------|
  | `view-surge.window` | `10s` | 속도 관측 창 |
  | `view-surge.threshold` | `200` | 창 안에서 이 건수를 넘으면 급상승 감지 |
  | `view-surge.sustain-threshold` | `100` | flush 델타가 이 값 이상이면 만료 연장 |
  | `view-surge.tracking-ttl` / `view-surge.extension` | `5m` / `5m` | 최초 만료 / 연장 폭 |
  | `view-surge.grace` | `15s` | 만료 후 정리까지의 유예 |
  | `view-surge.routing-refresh-interval` / `view-surge.flush-interval` / `view-surge.cleanup-interval` | `3s` / `3s` / `10s` | 스케줄 주기 |
  | `view-surge.routing-cache-max-size` / `view-surge.cleanup-batch-size` | `100000` / `100` | 스캔 LIMIT |

  값을 바꿔서 측정하려면 `--view-surge.threshold=...` 형태의 실행 인자
  (컨테이너면 `VIEW_SURGE_THRESHOLD` 환경변수)로 덮고, 바꾼 값을 결과 파일에 적습니다.
- [ ] 워밍업 — 본 측정과 같은 조건으로 1회 선행 실행(버퍼 풀·JIT). 이 결과는 버립니다.
      ```bash
      script/view-surge/run.sh bench -e VERSION=v3 -e RATE=100 -e DURATION=1m
      ```
- [ ] 결과 파일 생성 —
      `cp script/view-surge/results/TEMPLATE.md script/view-surge/results/$(date +%F)-surge.md`
- [ ] 결과 파일 상단의 측정 환경(커밋 해시, 인프라 사양, Redis 사양, 시드 규모, 서버 설정값) 기입
- [ ] 락 카운터 기준값 기록 — `lock-waits.sql` 의 [4]절

`BASE_URL` 은 로컬이면 `http://localhost:8080`(기본값이라 생략 가능), 원격이면 ALB 도메인.

### Step 1. V3 계단 부하 — 한계선 찾기 (표 1)

단일 게시글에 도착률을 계단으로 올려, MySQL 원자적 UPDATE가 무너지는 지점을 찾습니다.
이 수치가 급상승 감지 임계값의 근거입니다.

```bash
script/view-surge/run.sh bench -e VERSION=v3 -e EXECUTOR=steps \
       -e STEP_RATES=50,100,150,200,250,300 -e STEP_DURATION=1m
```

- 계단별 지연은 summary 의 `view_count_duration{step:rNNN}` sub-metric 에서 읽습니다.
  계단 판정은 테스트 시작 후 경과 시간으로 하므로, `STEP_DURATION` 을 바꿔도 자동으로 맞습니다.
- 계단별 **실측 RPS** 는 sub-metric 으로 나오지 않습니다. HTML 리포트의 시계열을
  계단 경계로 잘라 읽습니다(리포트 x축 = 경과 시간, 계단 경계 = `STEP_DURATION` 배수).
- 각 계단이 도는 동안 별도 세션에서 `lock-waits.sql` 1~3절을 실행해 대기 체인을 캡처합니다.
- p99 가 꺾이고 에러율이 오르는 계단이 한계선입니다. 그 값과 기본 임계값(200)의 관계를
  결과 파일에 적습니다 — 임계값이 한계선보다 높으면 감지가 늦어 이미 무너진 뒤에 전환됩니다.

### Step 2. V3 vs V4 상시 오버헤드 (표 2)

급상승이 아닌 평상시 트래픽. 쓰기가 흩어져 있으므로 두 버전의 차이는 감지 로직의 비용뿐입니다.

```bash
for mode in profile zipf; do
  for v in v3 v4; do
    script/view-surge/run.sh bench -e VERSION=$v -e POST_MODE=$mode \
           -e RATE=300 -e DURATION=2m
  done
done
```

- 이 구간에서 `view_surge_activation_total` 이 증가했다면 배경 트래픽만으로 감지가
  유발된 것입니다. 그 사실을 기록하고, 임계값 재검토 대상으로 남깁니다.

### Step 3. 라이프사이클 + 역주행 (표 3, 표 4)

```bash
# 표 3 — 최신글 급상승 (약 14.5분)
script/view-surge/run.sh lifecycle -e HOT_POST_ID=5999999 -e TAIL=8m

# 표 4 — 역주행: 오래된 글(일반 200만 시드 구간)이 갑자기 뜨는 경우
script/view-surge/run.sh lifecycle -e HOT_POST_ID=3000500 -e TAIL=8m
```

실행 **직전에 시작 시각을 기록**해 두세요. 모든 전이 시점을 이 t0 기준 경과로 환산합니다.

> **TAIL 을 왜 늘리는가**: 만료 시각은 마지막 연장(flush 델타 ≥ `sustain-threshold` 100,
> 대략 감쇠 중반)에서 다시 5분 뒤로 밀리고, 정리는 거기서 `grace` 15초를 더 기다립니다.
> 기본 TAIL(3m)이면 k6 가 먼저 끝나 복귀 구간이 시계열에 안 남습니다. 서버의
> `view-surge.tracking-ttl` 을 낮춰 측정해도 되지만, 그러면 "기본 설정에서의 복귀 시간"이
> 아니게 되므로 결과 파일에 반드시 적어야 합니다.

#### 감지·전환·복귀 시점은 3개를 교차 확인해 결정합니다

k6 하나로는 "언제 전환됐는지"를 확정할 수 없습니다. 어느 하나가 다른 둘과 어긋나면
그 사실 자체가 결과이므로, 반드시 셋 다 뜹니다.

| 근거 | 무엇을 보는가 | 어떻게 |
|------|---------------|--------|
| (a) Grafana / Prometheus 메트릭 | `view_surge_activation_total` 이 튀는 순간 = 감지.<br>`view_count_redis_path_total` 기울기가 요청량과 같아지는 순간 = 전환 완료.<br>`view_surge_cleanup_total` 증가 = 복귀. | `script/aws/tunnel.sh start` → http://localhost:3000 |
| (b) `view_surge_tracking` 타임스탬프 | `activated_at` = 감지 시각, `expires_at` 변화 = 연장 이력. 초 단위 확정값. | `explain/verify-integrity.sql` [2]절을 부하 중 주기적으로 실행 |
| (c) k6 `hot_duration` p99 시계열 | 지연이 실제로 떨어진 시점 = **사용자가 체감한** 전환 시각. | `results/raw/*.html` 리포트 |

(a)와 (b)는 서버가 "전환했다고 생각하는" 시각이고, (c)는 사용자가 실제로 이득을 본 시각입니다.
둘 사이의 간격(라우팅 캐시 갱신 지연 ~3초 + 인스턴스별 반영 편차)이 이 편에서 보여줄 수치입니다.

부하가 도는 동안 별도 세션에서:

`script/aws/tunnel.sh start` 가 MySQL(3306)과 Redis(6379)를 로컬로 포워딩하므로,
아래 명령은 **로컬에서** 그대로 돕니다.

```bash
# 추적 상태 폴링 (감지·연장 시각 확보)
watch -n 2 'mysql -h127.0.0.1 -P3306 -ucluverse_user -p<암호> cluverse_v2 \
  -e "SELECT post_id, activated_at, expires_at, NOW() FROM view_surge_tracking;"'

# Redis 누적 확인 (flush 가 3초마다 0 으로 리셋하므로 값이 오르내린다)
watch -n 2 'redis-cli -h 127.0.0.1 GET view:pending:5999999'
```

부하 중 참고할 메트릭(전부 `/actuator/prometheus`):

| 메트릭 | 의미 |
|--------|------|
| `view_surge_activation_total` | 급상승 감지 횟수 |
| `view_surge_extension_total` | 만료 연장 횟수 (급상승이 유지되고 있다는 신호) |
| `view_count_redis_path_total` | Redis 경로로 처리된 증가 요청 수 |
| `view_count_redis_fallback_total{origin="request"\|"flush"\|"cleanup"}` | Redis 실패로 폴백·중단한 횟수. `origin` 으로 요청/flush/정리 경로 구분 |
| `view_surge_flush_batch_size` (`_count`/`_sum`/`_max`) | flush 한 번이 처리한 게시글 수 |
| `view_surge_flush_duration_seconds` | flush 소요 시간. flush 주기(3s)에 근접하면 워커가 밀리는 중 |
| `view_surge_flush_restored_total` | MySQL 반영 실패로 Redis 에 되돌린 증가분 건수 |
| `view_surge_routing_cache_size` / `view_surge_sample_cache_size` | 라우팅 캐시 / 속도 샘플 캐시 크기 |
| `view_surge_cleanup_total` | 정리 완료 게시글 수 |
| `view_surge_sample_rebaselined_total` | 관측 창이 끊겨 속도 샘플을 재시작한 횟수 |

TAIL 구간(hotspot 종료 후 배경만 도는 3분)에는 정리가 일어나야 합니다.
Redis 키와 추적 행이 사라지는 시각을 복귀 시점으로 기록합니다.

### Step 4. Redis 장애 fallback (표 5)

전환이 끝난 상태에서 Redis 를 내려, 앱이 MySQL 경로로 떨어지는지 확인합니다.
**요청이 실패하지 않는 것**이 판정 기준입니다(성능 저하는 허용).

```bash
# 터미널 1 — 충분히 긴 급상승을 유지
script/view-surge/run.sh lifecycle -e HOT_POST_ID=5999999 -e SUSTAIN=6m

# 터미널 2 — 전환 확인 후(램프 시작 1~2분 뒤) Redis 중단.
# redis 서비스는 bastion 이 아니라 Redis EC2(10.0.11.20)에서 돌아간다 — bastion 경유로 들어간다.
BASTION=$(terraform -chdir=terraform/test output -raw bastion_public_ip)
ssh -i cluverse-key -J ec2-user@$BASTION ec2-user@10.0.11.20 'sudo systemctl stop redis6'
# ... 1분 관찰 ...
ssh -i cluverse-key -J ec2-user@$BASTION ec2-user@10.0.11.20 'sudo systemctl start redis6'
```

- 중단/복구 시각을 기록해 k6 시계열과 맞춥니다.
- `view_count_redis_fallback_total` 이 증가하고 에러율이 0 을 유지하면 정상입니다.
- 중단 시점에 Redis 에 남아 있던 미flush 증가분은 유실됩니다. 유실량을 추정해
  표 6의 비고에 적습니다 — 이 기능의 트레이드오프를 정직하게 드러내는 숫자입니다.

### Step 5. 정합성 + EXPLAIN (표 6, 표 7)

부하를 모두 멈추고 정리가 끝난 뒤 최종 상태에서 판정합니다.

```bash
mysql ... < script/view-surge/explain/verify-integrity.sql
redis-cli -h 127.0.0.1 --scan --pattern 'view:pending:*'   # 비어 있어야 정상

for f in v4-increase-and-get v4-surge-upsert v4-tracking-scan v4-flush-update; do
  mysql ... < script/view-surge/explain/$f.sql
done
```

| 파일 | 확인할 것 |
|------|----------|
| v4-increase-and-get | PK 단건. UPDATE/SELECT LAST_INSERT_ID 가 같은 커넥션인지 |
| v4-surge-upsert | PK 충돌 판정. GREATEST 로 만료 되감기 방지 |
| v4-tracking-scan | `idx_expires_at` 사용 여부, filesort 없음 |
| v4-flush-update | PK 단건. V3 와 동일 플랜 (차이는 호출 빈도) |

원격 MySQL 은 프라이빗 서브넷이라 bastion 경유가 필요합니다:
`cd terraform/test && terraform output ssm_port_forward_examples`

### Step 6. 정리

```bash
script/aws/down.sh    # 시간당 과금 정지 (ALB 제외)
```

TEMPLATE의 표 1~7을 채우고 마지막 "관찰/해석" 절을 서술하면 끝입니다.

---

## 환경변수 레퍼런스

### run.sh (공통 래퍼)

| 변수 | 기본값 | 설명 |
|------|--------|------|
| `PRE_ALLOCATED_VUS` | `100` | VU 풀 초기 크기 (스크립트 기본값 50을 덮어씀) |
| `MAX_VUS` | `600` | VU 풀 상한 |
| `K6_WEB_DASHBOARD` / `_EXPORT` | 자동 | 대시보드 켜기 + `results/raw/` 리포트 저장 |

### view-surge-bench.k6.js

| 변수 | 기본값 | 설명 |
|------|--------|------|
| `VERSION` | (필수) | `v3` \| `v4` — URL 프리픽스 결정 |
| `BASE_URL` | `http://localhost:8080` | 대상 서버 |
| `POST_MODE` | `fixed` | `fixed`(단일 게시글) \| `profile`(세그먼트 분포) \| `zipf` |
| `POST_ID` | `5999999` | fixed 모드 대상 게시글 |
| `POST_ID_MIN` / `POST_ID_MAX` | `5000001` / `13999999` | 분포 모드 게시글 범위 |
| `EXECUTOR` | `constant` | `constant`(고정 도착률) \| `steps`(계단 부하) |
| `RATE` / `DURATION` | `100` / `1m` | constant 모드 도착률 / 지속 시간 |
| `STEP_RATES` | `50,100,150,200,250,300` | steps 모드 계단 목록(rps, 콤마 구분) |
| `STEP_DURATION` | `1m` | steps 모드 계단 하나의 유지 시간 |
| `GRACEFUL_STOP` | `5s` | 종료 시 진행 중 요청 대기 상한 |
| `PRE_ALLOCATED_VUS` / `MAX_VUS` | `50` / 자동 | VU 풀 크기 (run.sh 사용 시 100/600) |

### view-surge-lifecycle.k6.js

| 변수 | 기본값 | 설명 |
|------|--------|------|
| `VERSION` | `v4` | `v3` 로 주면 같은 파형을 baseline 에 먹여 비교 |
| `BASE_URL` | `http://localhost:8080` | 대상 서버 |
| `HOT_POST_ID` | `5999999` | 급상승 대상. 역주행이면 `3000001~5000000`, `--30m` 적재 시 `14000501` 권장 (시드 표 참고) |
| `BG_RATE` | `50` | 배경 트래픽 도착률(rps) |
| `BG_MODE` | `profile` | 배경 분포 — `profile` \| `zipf` |
| `POST_ID_MIN` / `POST_ID_MAX` | `5000001` / `13999999` | 배경 트래픽 게시글 범위 |
| `START_RATE` | `DECAY_TO` 값 | 램프 시작 도착률 |
| `RAMP_TARGET` | `300` | 급상승 정점 도착률(rps) |
| `RAMP_UP` | `30s` | 정점까지 올리는 시간 |
| `SUSTAIN` | `3m` | 정점 유지 (이 안에서 감지·전환이 일어나야 한다) |
| `DECAY_TO` / `DECAY` | `5` / `1m` | 감쇠 후 도착률 / 감쇠 시간 |
| `COOL` | `2m` | 저트래픽 유지 (TTL 경과 대기) |
| `TAIL` | `3m` | hotspot 종료 후 배경만 도는 구간 (정리 관찰). 기본 서버 설정에서 복귀까지 담으려면 `8m` 이상 |
| `GRACEFUL_STOP` | `30s` | 종료 시 진행 중 요청 대기 상한 |

배경 시나리오의 지속 시간은 위 값들의 합(hotspot 전체 + TAIL)으로 **자동 계산**됩니다.
기본값 기준 총 9분 30초, 권장하는 `TAIL=8m` 기준 14분 30초입니다.

### 공통 (k6/lib/traffic-profile.js)

| 변수 | 기본값 | 설명 |
|------|--------|------|
| `POST_SEGMENTS` | - | 세그먼트 분포 오버라이드(JSON). 아래 참고 |
| `ZIPF_S` | `1.1` | Zipf 지수. 클수록 최신글 쏠림이 강해진다 |

부하 모델은 전부 **개방 모델**(constant/ramping-arrival-rate)입니다. 응답이 밀려도
초당 도착률이 유지되므로 대기열 증폭이 그대로 지연 분포에 드러납니다.
(닫힌 모델(고정 VU 루프)은 응답이 느려지면 자동으로 부하가 줄어 경합이 가려집니다.)

---

## 트래픽 프로파일

이 편의 부하는 **3계층**입니다. 셋을 섞지 않는 것이 설계의 핵심입니다 —
배경에 핫키를 섞으면 "배경이 만든 급상승"과 "우리가 만든 급상승"이 구분되지 않아
감지 시점 해석이 불가능해집니다.

| 계층 | 스크립트 | 역할 |
|------|----------|------|
| fixed | bench `POST_MODE=fixed` / lifecycle hotspot | 단일 게시글 집중. 급상승의 실체 |
| profile | bench `POST_MODE=profile` / lifecycle `BG_MODE=profile` | 세그먼트 기반 현실 분포 |
| zipf | bench `POST_MODE=zipf` / lifecycle `BG_MODE=zipf` | 경계 계단이 없는 매끄러운 멱법칙 분포 |

세그먼트 분포는 대형 커뮤니티(디시인사이드/에펨코리아) 분석 기준으로,
depth(최신글 기준 순번)를 축으로 정의합니다.

| 세그먼트(depth) | 가중치 | 의미 |
|-----------------|--------|------|
| 1 ~ 20          | 45%    | 목록 1페이지에 노출 중인 최신글 (조회 집중 구간) |
| 21 ~ 200        | 30%    | 최근 며칠 내 글 + 개념글 후보 |
| 201 ~ 5000      | 15%    | 검색/북마크/외부 링크 유입 |
| 5001 ~ MAX      | 10%    | 롱테일 (버퍼 풀 미스 → 디스크 I/O 유발 구간) |

세그먼트 내부는 균등 분포이며, depth 는 `postId = POST_ID_MAX - (depth - 1)` 로 변환됩니다.

**Zipf 모드를 함께 두는 이유**: 세그먼트 분포는 구간 경계에서 확률이 계단처럼 꺾입니다.
급상승 감지는 "배경 대비 얼마나 튀는가"를 보는 기능이라, 그 계단이 임계값 근처에
인공적인 봉우리를 만들 수 있습니다. Zipf 는 경계 없이 매끄럽게 감쇠하므로 대조군이 됩니다.

> **Zipf 구현 주의**: 순위 3천만 개의 조화수 누적표는 사전 계산이 불가능하므로,
> 밀도 `f(x) ∝ x^-s` 를 연속분포로 보고 역CDF 를 닫힌 식으로 풉니다. 이산 Zipf 와
> 완전히 같지 않고, 오차는 순위가 작은 쪽(rank 1~3)에 몰립니다. 부하의 "모양"을
> 만드는 데는 충분하지만, **Zipf 계수 자체를 논증하는 근거로는 쓰지 마세요.**

오버라이드:

```bash
-e POST_SEGMENTS='[{"from":1,"to":1,"weight":80},{"from":2,"to":100,"weight":20}]'
-e ZIPF_S=1.3
```

---

## 시드 데이터

`docs/v1/ddl/test-data/` 시드 기준 (실행 순서·소요시간은 그쪽 README 참고):

| 시드 | post_id 범위 | 이 편에서의 용도 |
|------|--------------|------------------|
| `05_post_seed` (일반 게시판 200만) | 3000001 ~ 5000000 | 역주행 시나리오 (id상 가장 오래된 구간. 단 created_at은 시딩 시점 상대시간) |
| `05a` (핫보드 100만) | 5000001 ~ 6000000 | 기본 `HOT_POST_ID=5999999` |
| `05b` (핫보드 +700만) | 6000001 ~ 14000000 | 배경 분포 기본 상한 |
| `05d` (일반 게시판 +1600만) | 14000001 ~ 30000000 | 버퍼 풀 대비 데이터가 압도적으로 큰 상황. `--30m` 적재 시 **역주행 1순위** — created_at이 2024-01-01부터 결정적으로 깔려 초반부(예: `14000501`)가 날짜까지 진짜 오래된 글이다 |

> 05d 구간에서 `HOT_POST_ID`를 고를 때는 시퀀스(n = post_id − 14,000,000) 기준
> `MOD(n,1000)=500`(DELETED), `MOD(n,300)=150`(BLINDED) 글을 피할 것. `14000501`(n=501)은 안전.

- 기본 `POST_ID_MAX=13999999` 는 조회수 편과 맞춘 값입니다(05b 상한 14000000 에서 1 모자라지만,
  배경 분포 범위라 영향이 없습니다). 05d 까지 적재했다면 `-e POST_ID_MAX=30000000` 으로 넓힙니다.
- 최신 글 6000000 은 시드 규칙상 DELETED 이므로 `HOT_POST_ID` 로 쓰지 않습니다.

## 응답 형태 참고

모든 응답은 `ApiResponse` 래퍼(`{code, status, message, data}`), 조회수 증가는 `data: null`.
view-count 계열 엔드포인트는 부하테스트를 위해 비로그인 허용(WebMvcConfig excludePathPatterns).

## 트러블슈팅

| 증상 | 원인/해결 |
|------|-----------|
| `VERSION 은 v3\|v4 중 하나여야 합니다` 로 즉시 종료 | `-e VERSION=...` 누락/오타 (lifecycle 은 기본 v4) |
| `POST_MODE 는 fixed\|profile\|zipf ...` / `EXECUTOR 는 ...` | 오타. 값은 전부 소문자 취급 |
| `기간 표기를 해석할 수 없습니다` | `STEP_DURATION`/`SUSTAIN` 등에 단위 누락 (`3` ✗ → `3m` ✓) |
| 부하를 넣어도 `view_surge_tracking` 이 비어 있다 | 도착률이 임계값 미만. `RAMP_TARGET` 을 올리거나 서버 `VIEW_SURGE_THRESHOLD` 확인. 배경(BG_RATE)은 대상 게시글에 안 들어간다 |
| 감지는 됐는데 Redis 키가 안 생긴다 | 라우팅 캐시 갱신(~3초) 전이거나, 앱이 Redis 에 연결되지 않음 — `view_count_redis_fallback_total` 확인 |
| 전환 후에도 p99 가 안 떨어진다 | 병목이 조회수 UPDATE 가 아닐 수 있다. 커넥션 풀/CPU/네트워크를 Grafana 로 대조 |
| 정리가 안 되고 추적 행이 남는다 | k6 가 먼저 끝난 것뿐일 수 있다. 만료 연장 때문에 복귀는 감쇠 후 5분+15초 뒤다 — `-e TAIL=8m` 로 재실행하거나 부하 종료 후 몇 분 더 관찰 |
| MySQL 증가분 + Redis 잔여분 ≠ k6 성공 수 | 중간 시점 스냅샷은 flush 와 경쟁한다. 부하 정지 후 최종 상태로 판정 |
| 실측 RPS 가 목표 RATE 에 못 미침 | VU 풀 부족(`MAX_VUS` 증설) 또는 서버 포화 — 후자면 그게 측정 결과 |
| `dropped_iterations` 가 다수 | 램프 정점에서 VU 부족. run.sh 를 쓰거나 `PRE_ALLOCATED_VUS` 를 올린다 |
| EXPLAIN rows 추정이 터무니없다 | 대량 적재 후 통계 미갱신 — `ANALYZE TABLE` 실행 |
| `import` 문법 에러 | k6 버전이 낮음 — v0.50+ 로 업그레이드 |
