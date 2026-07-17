# 게시글 조회수 증가 V1~V3 성능 측정 도구

게시글 조회수 증가 API의 동시성 제어 전략(낙관적 락 → 비관적 락 → 원자적 UPDATE)을
**같은 조건으로 측정해 수치로 비교**하기 위한 k6 부하 스크립트 + EXPLAIN/락 관찰 SQL 모음입니다.

| 버전 | 엔드포인트 | 동시성 제어 전략 | 대상 테이블 |
|------|-----------|------------------|-------------|
| V1 | `POST /api/v1/posts/{postId}/view-count` | 낙관적 락 (@Version + 최대 10회 재시도) | `post_view_count_optimistic` |
| V2 | `POST /api/v2/posts/{postId}/view-count` | 비관적 락 (select for update + 더티체킹) | `post_view_count` |
| V3 | `POST /api/v3/posts/{postId}/view-count` | 원자적 UPDATE (운영 방식) | `post_view_count` |

```
script/view-count/
  k6/
    lib/traffic-profile.js      ← 게시글 선택 분포 (수치 단일 출처)
    view-count-bench.k6.js      ← V1~V3 공용 벤치 (fixed/profile 모드)
  explain/
    v1-optimistic.sql           ← V1 실제 쿼리 EXPLAIN (SELECT + version 조건 UPDATE)
    v2-pessimistic.sql          ← V2 실제 쿼리 EXPLAIN (SELECT FOR UPDATE + UPDATE)
    v3-atomic.sql               ← V3 실제 쿼리 EXPLAIN (원자적 UPDATE)
    lock-waits.sql              ← 부하 중 락 경합 관찰 스냅샷 (핵심 관찰 도구)
    verify-integrity.sql        ← 측정 전후 정합성 검증 (갱신 유실 0 입증)
  results/TEMPLATE.md           ← 측정 결과 기록 양식
```

**측정의 핵심**: 세 버전의 쿼리 플랜은 전부 PK 단건 접근으로 동일합니다.
차이는 **락 보유 시간**(V2: select~commit / V3: 문장 실행 순간)과
**경합 처리 방식**(V1: 대기 대신 재시도)에서 나오고, 그것은 EXPLAIN 이 아니라
부하 중 지연 분포와 `lock-waits.sql` 스냅샷에서 드러납니다.

---

## 빠른 시작 (로컬에서 5분 스모크)

측정 파이프라인이 동작하는지 로컬에서 먼저 확인합니다. 본 측정은 아래 [본 측정 절차](#본-측정-절차)로.

```bash
# 0) k6 설치 (v0.50+ 필요)
brew install k6

# 1) MySQL 기동 (리포 루트의 docker-compose 사용 시)
docker compose up -d db

# 2) 앱 기동 — 첫 기동 시 Flyway가 스키마를 자동 생성
./gradlew bootRun

# 3) 시드 적재 (순서대로. 05a까지 넣으면 핫보드에 100만 건)
cd docs/v1/ddl/test-data
mysql -h127.0.0.1 -ucluverse_user -ptest1234 cluverse_v2 < 01_university_seed.sql
# ... 02, 03, 04, 05, 05a 순서대로, 마지막에 05c (V1 측정 시 필수!)
mysql -h127.0.0.1 -ucluverse_user -ptest1234 cluverse_v2 < 05c_view_count_optimistic_seed.sql

# 4) 스모크: 초당 5회 × 10초씩 세 버전 확인
for v in v1 v2 v3; do
  k6 run -e VERSION=$v -e RATE=5 -e DURATION=10s script/view-count/k6/view-count-bench.k6.js
done
```

k6 요약 출력에서 아래 두 가지가 보이면 성공입니다.

- `view_count_success_rate` 가 100%에 가깝다
- `view_count_duration` 에 avg/p95/p99 값이 찍힌다

---

## 본 측정 절차

측정은 5단계입니다. 각 단계의 결과를 `results/` 에 바로 적으면서 진행하는 걸 권장합니다.

### Step 0. 준비 체크리스트

- [ ] 시드 적재 완료 — 05a(필수) + **05c(V1 낙관적 락 측정 시 필수)**.
      05c 없이 V1을 돌리면 첫 요청마다 insert 경합이 발생해 측정이 왜곡됩니다.
- [ ] 앱 기동 확인 — `curl -X POST "$BASE_URL/api/v3/posts/6000000/view-count"` 가 `"code":200` 반환
      (view-count 엔드포인트는 부하테스트용으로 비로그인 허용되어 있음)
- [ ] 결과 파일 생성 — `cp script/view-count/results/TEMPLATE.md script/view-count/results/$(date +%F)-hotrecord.md`
- [ ] 결과 파일 상단의 측정 환경(커밋 해시, 인프라 사양, 시드 규모, MySQL 버퍼풀, **HikariCP 풀 크기**) 기입
- [ ] 락 카운터 기준값 기록 — `lock-waits.sql` 의 [4]절(`Innodb_row_lock%`) 실행

`BASE_URL` 은 로컬이면 `http://localhost:8080`(기본값이라 생략 가능), 원격이면 ALB 도메인.

### Step 1. fixed 모드 — 단일 핫 레코드 경합 (핵심 측정)

게시글 하나(기본 6000000)에 쓰기를 집중시켜 락 전략별 경합 내성을 비교합니다.
RATE 단계(100/300/500)마다 버전만 바꿔 **완전히 동일한 조건**으로 돌립니다. 결과는 표 1에.

```bash
for rate in 100 300 500; do
  for v in v1 v2 v3; do
    k6 run -e VERSION=$v -e RATE=$rate -e DURATION=1m \
           script/view-count/k6/view-count-bench.k6.js
    # 각 실행 전후로 verify-integrity.sql 스냅샷 → 정합성 열 기록
  done
done
```

- 각 실행 **직전/직후**에 `verify-integrity.sql` 을 떠서 view_count 델타 == k6 성공 수를 확인합니다(정합성 열).
- V1 은 `view_count_retry_exhausted`(재시도 10회 소진 = 500 비율)를 표 3에 함께 기록합니다.
- V2 는 락 대기가 커넥션 풀을 잠식해 실측 RPS 가 목표 RATE 에 못 미칠 수 있습니다 — 그것 자체가 결과입니다.

### Step 2. profile 모드 — 현실 트래픽 분포

[트래픽 프로파일](#트래픽-프로파일) 분포로 게시글을 샘플해, 쓰기가 분산될 때
세 버전의 차이가 얼마나 줄어드는지 봅니다. 결과는 표 2에.

```bash
for v in v1 v2 v3; do
  k6 run -e VERSION=$v -e POST_MODE=profile -e RATE=300 -e DURATION=1m \
         script/view-count/k6/view-count-bench.k6.js
done
```

### Step 3. 락 관찰 — 부하 중 스냅샷 (스크린샷 대상)

Step 1 의 fixed 부하가 돌아가는 동안 **별도 세션**에서 `lock-waits.sql` 1~3절을 실행해
락 대기 체인을 캡처합니다. 부하 종료 후 [4]절 누적 카운터의 전/후 델타를 표 4에 기록합니다.

```bash
# 부하 중 (다른 터미널에서)
mysql -h127.0.0.1 -ucluverse_user -ptest1234 cluverse_v2 \
      < script/view-count/explain/lock-waits.sql
```

관전 포인트: **V2 는 대기 행이 다수 + row_lock_time 델타가 크고, V3 는 거의 0,
V1 은 대기 대신 재시도(표 3)로 나타나면** 락 보유 시간 서사가 실측으로 입증된 것입니다.

### Step 4. EXPLAIN 캡처

`explain/v*-*.sql` 은 각 버전이 실제 날리는 쿼리입니다. 실행 결과를 표 5에 기록합니다.
세 버전 모두 PK 단건 접근으로 **플랜이 같음**을 확인하는 것이 목적입니다.

```bash
# 로컬
mysql -h127.0.0.1 -ucluverse_user -ptest1234 cluverse_v2 \
      < script/view-count/explain/v3-atomic.sql

# 원격 (MySQL이 프라이빗 서브넷 → bastion 경유 SSM 포트포워딩)
cd terraform/test
terraform output ssm_port_forward_examples   # 3306 포워딩 명령 확인 후 실행
mysql -h127.0.0.1 -P <forwarded_port> -ucluverse_user -p cluverse_v2 \
      < script/view-count/explain/v1-optimistic.sql
```

| 파일 | 확인할 것 |
|------|----------|
| v1-optimistic | SELECT/UPDATE 모두 PK 단건. version 불일치 시 0 row → 재시도 |
| v2-pessimistic | 플랜은 V3와 동일 — 차이는 락 보유 시간 (lock-waits 로 관찰) |
| v3-atomic | PK 단건, 문장 단위 락. EXPLAIN ANALYZE 는 단일 테이블 UPDATE 미지원 |

### Step 5. 결과 정리

TEMPLATE의 표 1(fixed) → 표 2(profile) → 표 3(재시도) → 표 4(락 관찰) → 표 5(EXPLAIN) 를
채우고, 마지막 "관찰/해석" 절에 버전 간 차이를 서술합니다.

---

## 환경변수 레퍼런스

### view-count-bench.k6.js (V1~V3)

| 변수 | 기본값 | 설명 |
|------|--------|------|
| `VERSION` | (필수) | `v1` \| `v2` \| `v3` — URL 프리픽스 결정 |
| `BASE_URL` | `http://localhost:8080` | 대상 서버 |
| `POST_MODE` | `fixed` | `fixed`(단일 핫 레코드) \| `profile`(분포 샘플) |
| `POST_ID` | `6000000` | fixed 모드 대상 게시글 (05a 시드 최신 글) |
| `POST_ID_MIN` / `POST_ID_MAX` | `5000001` / `6000000` | profile 모드 게시글 범위 (05a 기준) |
| `RATE` / `DURATION` | `100` / `1m` | 초당 요청 수 / 지속 시간 |
| `PRE_ALLOCATED_VUS` / `MAX_VUS` | 50 / 자동 | VU 풀 크기 |
| `POST_SEGMENTS` | - | 트래픽 분포 오버라이드(JSON). 아래 참고 |

부하 모델은 `constant-arrival-rate`(개방 모델)입니다. 락 대기로 응답이 밀려도
초당 도착률이 유지되므로, 대기열 증폭이 그대로 지연 분포에 드러납니다.
(닫힌 모델(고정 VU 루프)은 응답이 느려지면 자동으로 부하가 줄어 경합이 가려집니다.)

---

## 트래픽 프로파일

대형 커뮤니티 분석(디시인사이드/에펨코리아) 기준, 조회는 목록 1페이지에 노출 중인
최신글에 집중되고, 소수의 과거글이 검색·외부 링크로 조회되는 롱테일을 만듭니다.
이 분포를 `k6/lib/traffic-profile.js` **한 곳에서** depth(최신글 기준 순번) 세그먼트로 정의합니다.

| 세그먼트(depth) | 가중치 | 의미 |
|-----------------|--------|------|
| 1 ~ 20          | 45%    | 목록 1페이지에 노출 중인 최신글 (조회 집중 구간) |
| 21 ~ 200        | 30%    | 최근 며칠 내 글 + 개념글 후보 |
| 201 ~ 5000      | 15%    | 검색/북마크/외부 링크 유입 |
| 5001 ~ MAX      | 10%    | 롱테일 (버퍼 풀 미스 → 디스크 I/O 유발 구간) |

세그먼트 내부는 균등 분포이며, depth 는 `postId = POST_ID_MAX - (depth - 1)` 로 변환됩니다.
인기글(단일 레코드 집중)은 분포가 아니라 **fixed 모드**로 따로 측정합니다 —
인기글 전용 부하 설계(승격 트리거·캐시)는 인기글 편에서 다룹니다.

오버라이드:

```bash
-e POST_SEGMENTS='[{"from":1,"to":1,"weight":80},{"from":2,"to":100,"weight":20}]'
```

---

## 시드 데이터

`docs/v1/ddl/test-data/` 시드 기준 (실행 순서·소요시간은 그쪽 README 참고):

| 시드 | 내용 | post_id 범위 |
|------|------|--------------|
| `05a_popular_board_post_seed.sql` | 핫보드 2001001 에 100만 건 | 5000001 ~ 6000000 |
| `05b_popular_board_post_seed_8m.sql` | 누적 800만 건 (선택) | 6000001 ~ 13999999 |
| `05c_view_count_optimistic_seed.sql` | **post_view_count_optimistic 사전 적재 (V1 측정 시 필수)** | post_view_count 복제 |

- 기본 `POST_ID=6000000` 은 05a 의 최신 글 = "목록 1페이지 최상단의 핫 레코드" 역할.
- 05b 까지 넣었다면 `-e POST_ID=13999999 -e POST_ID_MAX=13999999` 로 조정.
- 05a/05b 를 다시 실행했다면 **05c 도 다시 실행**해야 합니다.

## 응답 형태 참고

모든 응답은 `ApiResponse` 래퍼(`{code, status, message, data}`), 조회수 증가는 `data: null`.
view-count 엔드포인트는 부하테스트를 위해 비로그인 허용(WebMvcConfig excludePathPatterns).

## 트러블슈팅

| 증상 | 원인/해결 |
|------|-----------|
| `VERSION 은 v1\|v2\|v3 중 하나여야 합니다` 에러로 즉시 종료 | `-e VERSION=...` 누락/오타 |
| V1 에서 500 이 다발 | **재시도 10회 소진 — 고경합에서 정상적인 관찰 신호.** `view_count_retry_exhausted` 로 비율 기록 |
| V2/V3 인데 500 발생 | 비정상. 서버 로그 확인 (V2 락 타임아웃 `innodb_lock_wait_timeout` 초과 가능성) |
| 응답이 전부 404 "존재하지 않는 게시글" | 시드 미적재 또는 `POST_ID` 오타 — 05a 시드 확인 |
| 응답이 401/403 | WebMvcConfig 의 view-count excludePathPatterns 누락 — 브랜치/배포 버전 확인 |
| V1 이 비정상적으로 느림 (insert 경합) | 05c 시드 미적재 — post_view_count_optimistic 사전 적재 필수 |
| 실측 RPS 가 RATE 에 못 미침 | VU 풀 부족(`MAX_VUS` 증설) 또는 서버 커넥션 풀 포화 — 후자면 그게 측정 결과 |
| `import` 문법 에러 | k6 버전이 낮음 — v0.50+ 로 업그레이드 |
