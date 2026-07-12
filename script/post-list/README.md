# 게시글 목록 조회 V1~V4 성능 측정 도구

게시글 목록 조회 API의 개선 단계(offset → deferred join → 상한 COUNT → 커서)를
**같은 트래픽 분포로 측정해 수치로 비교**하기 위한 k6 부하 스크립트 + EXPLAIN SQL 모음입니다.

| 버전 | 엔드포인트 | 개선 포인트 | page 상한 |
|------|-----------|-------------|-----------|
| V1 | `GET /api/v1/posts` | naive offset 풀 조인 + 전체 COUNT (비교 기준) | 20000 |
| V2 | `GET /api/v2/posts` | 커버링 인덱스 deferred join + 전체 COUNT | 20000 |
| V3 | `GET /api/v3/posts` | V2 + 페이지 블록 상한 COUNT | **500** |
| V4 | `GET /api/v4/posts` | 날짜 앵커 + 튜플 커서 (offset 제거) | 커서 기반 |

```
script/post-list/
  k6/
    lib/traffic-profile.js      ← 페이지 깊이 분포 (수치 단일 출처)
    post-list-bench.k6.js       ← V1~V3 공용 벤치 (profile/fixed 모드)
    post-list-cursor.k6.js      ← V4 커서 세션 플로우
  explain/                      ← 각 버전이 실제 날리는 쿼리의 EXPLAIN 9종
  results/TEMPLATE.md           ← 측정 결과 기록 양식
```

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

# 3) 시드 적재 (순서대로. 05a까지 넣으면 핫보드 2001001에 100만 건)
cd docs/v1/ddl/test-data
mysql -h127.0.0.1 -ucluverse_user -ptest1234 cluverse_v2 < 01_university_seed.sql
# ... 02, 03, 04, 05, 05a 순서대로 (자세한 순서/소요시간: docs/v1/ddl/test-data/README.md)

# 4) 스모크: 초당 5회 × 10초만 쏴본다
k6 run -e VERSION=v3 -e RATE=5 -e DURATION=10s script/post-list/k6/post-list-bench.k6.js
```

k6 요약 출력에서 아래 두 가지가 보이면 성공입니다.

- `post_list_success_rate` 가 100%에 가깝다
- `post_list_duration` 에 avg/p95/p99 값이 찍힌다

---

## 본 측정 절차

측정은 5단계입니다. 각 단계의 결과를 `results/` 에 바로 적으면서 진행하는 걸 권장합니다.

### Step 0. 준비 체크리스트

- [ ] 시드 적재 완료 — **핫보드 2001001에 100만+ 건** (05a 필수, 05b까지 넣으면 700만)
- [ ] 앱 기동 확인 — `curl "$BASE_URL/api/v3/posts?boardId=2001001" | head` 가 게시글 JSON을 반환
- [ ] 결과 파일 생성 — `cp script/post-list/results/TEMPLATE.md script/post-list/results/$(date +%F)-hotboard.md`
- [ ] 결과 파일 상단의 측정 환경(커밋 해시, 인프라 사양, 시드 규모, MySQL 버전/버퍼풀) 기입

`BASE_URL` 은 로컬이면 `http://localhost:8080`(기본값이라 생략 가능), 원격이면 ALB 도메인.

### Step 1. V1~V3 profile 모드 — 실사용 분포 혼합 부하

버전만 바꿔가며 **완전히 동일한 조건**으로 세 번 돌립니다. 결과는 TEMPLATE의 표 1에.

```bash
for v in v1 v2 v3; do
  k6 run -e VERSION=$v -e RATE=200 -e DURATION=2m \
         script/post-list/k6/post-list-bench.k6.js
done
```

- `RATE`(초당 요청 수)는 환경에 맞게 조절. 대상이 죽지 않는 선에서 세 버전 동일하게.
- 페이지는 [트래픽 프로파일](#트래픽-프로파일) 분포로 자동 샘플됩니다.

### Step 2. V1~V3 fixed 모드 — offset 구간 프로브 (그래프용)

고정 페이지를 반복 조회해 "뒷 페이지로 갈수록 얼마나 느려지는가"를 구간별로 잽니다. 표 2에.

```bash
# offset 0 / 10만 / 40만 = page 1 / 5000 / 20000 (size=20 기준)
for v in v1 v2; do
  for p in 1 5000 20000; do
    k6 run -e VERSION=$v -e PAGE_MODE=fixed -e FIXED_PAGE=$p \
           -e RATE=50 -e DURATION=1m script/post-list/k6/post-list-bench.k6.js
  done
done
# V3는 page 상한이 500이므로 FIXED_PAGE=1, 100, 500 정도로
```

### Step 3. V4 커서 세션 플로우

iteration 하나가 사용자 세션 하나입니다: 진입 → 응답의 `nextCursor` 로 NEXT 이동을 반복.
이동 깊이는 Step 1과 같은 분포에서 샘플되므로 공정 비교가 됩니다.

```bash
# 무앵커(최신) 진입
k6 run -e RATE=100 -e DURATION=2m script/post-list/k6/post-list-cursor.k6.js

# 날짜 앵커 진입
k6 run -e DATE=2024-06-01 -e RATE=100 -e DURATION=2m \
       script/post-list/k6/post-list-cursor.k6.js
```

관전 포인트: `cursor_step_duration` 이 depth 태그(d01, d02-05, … d51+)별로 나오는데,
**깊이가 깊어져도 응답 시간이 늘지 않으면** "커서는 O(1)"이 입증된 것입니다. 표 2-b에.

### Step 4. EXPLAIN 캡처

`explain/*.sql` 은 각 버전이 실제 날리는 쿼리입니다. 파일 상단에 파라미터(`SET @board_id` 등)와
"확인 포인트" 주석이 있으니, 실행 결과에서 해당 항목을 확인해 표 3에 기록합니다.

```bash
# 로컬
mysql -h127.0.0.1 -ucluverse_user -ptest1234 cluverse_v2 \
      < script/post-list/explain/v2-ids.sql

# 원격 (MySQL이 프라이빗 서브넷 → bastion 경유 SSM 포트포워딩)
cd terraform/test
terraform output ssm_port_forward_examples   # 3306 포워딩 명령 확인 후 실행
mysql -h127.0.0.1 -P <forwarded_port> -ucluverse_user -p cluverse_v2 \
      < script/post-list/explain/v1-list.sql
```

버전별 핵심 확인 포인트:

| 파일 | 확인할 것 |
|------|----------|
| v1-list | offset을 키우면 rows(읽는 행)가 비례해서 커진다 |
| v1-count / v2-count | 전체 COUNT의 인덱스 스캔 행 수 |
| v2-ids | Extra = `Using index` (커버링 인덱스) |
| v2-projection | IN 30건만 클러스터드 인덱스 접근 (※ IN 목록은 예시값 — v2-ids 결과로 교체) |
| v3-count | 파생 테이블 LIMIT에서 스캔이 멈춘다 (v1-count와 rows 비교) |
| v4-entry / v4-next / v4-prev | type = `range`, actual rows ≈ size+1 (깊이 무관) |

주의: MySQL은 `LIMIT`/`OFFSET` 에 사용자 변수를 못 쓰므로 그 값만 리터럴입니다.
offset/page를 바꿔 실험하려면 파일 안 숫자를 직접 수정하세요.

### Step 5. 결과 정리

TEMPLATE의 표 1(profile) → 표 2/2-b(fixed·커서) → 표 3(EXPLAIN) 을 채우고,
마지막 "관찰/해석" 절에 버전 간 차이를 서술합니다.

---

## 환경변수 레퍼런스

### post-list-bench.k6.js (V1~V3)

| 변수 | 기본값 | 설명 |
|------|--------|------|
| `VERSION` | (필수) | `v1` \| `v2` \| `v3` — URL 프리픽스 결정 |
| `BASE_URL` | `http://localhost:8080` | 대상 서버 |
| `BOARD_ID` | `2001001` | 대상 게시판 (기본 = 핫보드) |
| `PAGE_MODE` | `profile` | `profile`(분포 샘플) \| `fixed`(고정 페이지) |
| `FIXED_PAGE` | `1` | fixed 모드의 고정 페이지 |
| `MAX_PAGE` | v3=500, v1/v2=20000 | profile 모드 페이지 상한 |
| `SIZE` | `20` | 페이지 크기 |
| `SORT` / `CATEGORY` | - | 정렬(LATEST\|VIEW_COUNT) / 카테고리 필터 |
| `RATE` / `DURATION` | `100` / `1m` | 초당 요청 수 / 지속 시간 |
| `PRE_ALLOCATED_VUS` / `MAX_VUS` | 50 / 자동 | VU 풀 크기 |

### post-list-cursor.k6.js (V4)

| 변수 | 기본값 | 설명 |
|------|--------|------|
| `BASE_URL` / `BOARD_ID` / `SIZE` / `RATE` / `DURATION` | 위와 동일 | |
| `DATE` | - | 날짜 앵커 진입 (`yyyy-MM-dd`). 생략 시 최신 진입 |
| `CURSOR_MAX_DEPTH` | `100` | 세션당 최대 NEXT 이동 횟수 (분포 샘플의 상한) |

### 공통

| 변수 | 설명 |
|------|------|
| `PAGE_SEGMENTS` | 트래픽 분포 오버라이드(JSON). 아래 참고 |

---

## 트래픽 프로파일

실제 게시판은 조회가 앞페이지에 극단적으로 편중되고, 소수의 깊은 조회가 꼬리 지연(p99)을 만듭니다.
이 분포를 `k6/lib/traffic-profile.js` **한 곳에서** 세그먼트 가중치로 정의하고,
bench(offset)와 cursor(depth)가 같은 분포를 재사용합니다.

| 세그먼트(page) | 가중치 | 의미 |
|----------------|--------|------|
| 1              | 50%    | 최신글만 보고 이탈하는 다수 |
| 2 ~ 10         | 30%    | 한두 번 넘겨보는 사용자 |
| 11 ~ 100       | 15%    | 특정 주제/과거글 탐색 |
| 101 ~ MAX_PAGE | 5%     | 깊은 탐색 (offset 페널티의 원인) |

세그먼트 내부는 균등 분포이며, 오버라이드는:

```bash
-e PAGE_SEGMENTS='[{"from":1,"to":1,"weight":70},{"from":2,"to":50,"weight":30}]'
```

---

## 시드 데이터와 보드 선택

`docs/v1/ddl/test-data/` 시드 기준 (실행 순서·소요시간은 그쪽 README 참고):

| 시드 | board_id | post_id 범위 | post 건수 |
|------|----------|--------------|-----------|
| `05_post_seed.sql` | 2000001 ~ 2000120 | 3000001 ~ 5000000 | 200만 (120개 보드 분산 → 보드당 ~1.6만) |
| `05a_popular_board_post_seed.sql` | **2001001** | 5000001 ~ 6000000 | **100만** (단일 핫보드) |
| `05b_popular_board_post_seed_8m.sql` | **2001001**(누적), 2001002 | 6000001 ~ 13999999 | 2001001 누적 **700만**, 2001002 200만 |

- **기본값 = 핫보드 2001001.** 깊은 offset(page 20000 ≈ offset 40만)과 100만+ COUNT 측정은
  단일 보드에 100만+ 행이 있어야 재현됩니다. k6와 EXPLAIN(`SET @board_id`) 모두
  별도 지정 없이 그대로 실행하면 이 조건이 됩니다.
- **저밀도 일반 보드**를 보고 싶으면 `-e BOARD_ID=2000001`. 단 보드당 ~1.6만 건이라
  깊은 offset 프로브는 불가능하고 앞페이지 위주로만 의미가 있습니다.

---

## 응답 형태 참고

모든 응답은 `ApiResponse` 래퍼(`{code, status, message, data}`).

- V1~V3 `data`: `{posts, page, size, hasNext, lastPage, hasNextBlock, dateBased}`
- V4 `data`: `{posts, size, hasNext, hasPrev, prevCursor:{createdAt,postId}, nextCursor:{createdAt,postId}}`
- 목록 조회는 인증 불필요(비로그인 가능) — k6에서 세션 처리 없음.

## 트러블슈팅

| 증상 | 원인/해결 |
|------|-----------|
| `VERSION 은 v1\|v2\|v3 중 하나여야 합니다` 에러로 즉시 종료 | bench 스크립트에 `-e VERSION=...` 누락/오타 |
| 응답이 전부 400 | V3에 `FIXED_PAGE > 500` 을 준 경우 (V3 page 상한 500) |
| 응답이 전부 404 "존재하지 않는 게시판" | 시드 미적재 또는 `BOARD_ID` 오타 — 05a 시드와 2001001 확인 |
| 뒷 페이지인데 posts가 빈 배열 | 해당 보드 게시글 수 < offset — 핫보드(2001001)인지 확인 |
| `import` 문법 에러 | k6 버전이 낮음 — v0.50+ 로 업그레이드 |
