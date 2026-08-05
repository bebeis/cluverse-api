# Test Data Seed Scripts

MySQL 8.x 기준 대량 테스트데이터 삽입 스크립트입니다.

## 대상 수량

- `university`: 30건
- `member`: 50,000건
- `major`: 300건
- `interest`: 200건
- `post`: 2,000,000건
- `popular board post`: 1,000,000건
- `general board long-tail post`(05d, 선택): 16,000,000건
- `comment`: 3,000,000건
- `follow`: 250,000건
- `block`: 25,000건

## 파일 목록

- `01_university_seed.sql`
- `02_member_seed.sql`
- `03_major_seed.sql`
- `04_interest_seed.sql`
- `05_post_seed.sql`
- `05a_popular_board_post_seed.sql`
- `05b_popular_board_post_seed_8m.sql`
- `05d_post_seed_30m.sql` - 일반 게시판(2000001~2000120)에 1,600만 건 추가 → post_id 상한 3,000만
- `06_comment_seed.sql`
- `07_follow_seed.sql`
- `08_block_seed.sql`
- `09_realistic_korean_seed.sql` - 실제 한국 대학/관심사/모임 분위기의 소규모 샘플 데이터

## 실행 순서

1. `01_university_seed.sql`
2. `02_member_seed.sql`
3. `03_major_seed.sql`
4. `04_interest_seed.sql`
5. `05_post_seed.sql`
6. `05a_popular_board_post_seed.sql` (선택)
7. `05b_popular_board_post_seed_8m.sql` (선택, `05a` 이후)
8. `05d_post_seed_30m.sql` (선택, `05` 이후 — `05a`/`05b`와는 순서 무관)
9. `06_comment_seed.sql`
10. `07_follow_seed.sql`
11. `08_block_seed.sql`

실서비스형 샘플이 필요하면 대량 데이터 스크립트 대신 `09_realistic_korean_seed.sql`만 단독 실행해도 됩니다.

## 예상 소요시간

측정 스택(t3.small + gp3 30GB, 버퍼풀 기본값) 기준입니다.

- `05d_post_seed_30m.sql`: **`05b` 소요의 약 2.5~3.5배** — 실측이 아니라 05b 기준 추정입니다.
  행수는 05b(800만)의 2배지만, 보드 120개 × 기간 2.5년으로 흩어 넣기 때문에
  `(board_id, status, created_at, post_id)` 인덱스 삽입이 랜덤이 되어 단순 2배보다 더 걸립니다.
  실측치가 나오면 이 줄을 실제 값으로 대체하세요.
- 디스크: 05d는 `post` 약 6GB + 카운트 4종 약 3GB ≈ **+9GB**를 씁니다. MySQL EBS가 30GB이므로
  05/05a/05b까지 넣은 상태에서 추가할 때는 적재 전 `df -h`로 여유를 확인하고,
  바이너리 로그(MySQL 8 기본 ON)가 수 GB 쌓이면 `PURGE BINARY LOGS`로 회수하세요.

## 의존성

- `member`는 `university`를 참조하므로 `university` 이후에 실행해야 합니다.
- `major`는 `member_major`를 함께 생성하므로 `member` 이후에 실행해야 합니다.
- `interest`는 `major`, `member`를 참조하므로 `major` 이후에 실행해야 합니다.
- `post`는 `member`를 참조하므로 `member` 이후에 실행해야 합니다.
- `05a_popular_board_post_seed.sql`은 `member` 이후에 실행 가능하며, 일반 게시판 데이터와 함께 쓰려면 `05_post_seed.sql` 다음에 실행하는 것이 좋습니다.
- `comment`는 `post`, `member`를 참조하므로 `post` 이후에 실행해야 합니다.
- `follow`, `block`은 `member`만 참조하므로 `member` 이후면 어느 시점에 실행해도 됩니다.
- `05d_post_seed_30m.sql`은 `05_post_seed.sql`이 만든 일반 게시판(`2000001`~`2000120`)에 글만 덧붙이므로 `05` 이후여야 하고, `05a`/`05b`와는 board·post_id 범위가 겹치지 않아 순서를 따지지 않습니다.

## 재실행 주의사항

- 상위 도메인 스크립트를 다시 실행하면 하위 도메인 데이터의 참조 일관성이 깨질 수 있습니다.
- 예를 들어 `05_post_seed.sql`을 다시 실행했다면 `06_comment_seed.sql`도 다시 실행해야 합니다.
- `05a_popular_board_post_seed.sql`을 다시 실행하는 경우에는 `06_comment_seed.sql` 재실행이 필수는 아니지만, 인기 게시판 글에도 댓글 데이터를 붙일 계획이라면 별도 후속 스크립트가 필요합니다.
- `05b_popular_board_post_seed_8m.sql`은 `05a_popular_board_post_seed.sql`이 만든 인기 게시판을 전제로 하므로, `05a`를 다시 실행했다면 `05b`도 다시 실행하는 편이 안전합니다.
- 같은 이유로 `02_member_seed.sql`을 다시 실행했다면 `03_major_seed.sql`, `04_interest_seed.sql`, `05_post_seed.sql`, `05a_popular_board_post_seed.sql`, `05b_popular_board_post_seed_8m.sql`, `06_comment_seed.sql`, `07_follow_seed.sql`, `08_block_seed.sql`도 다시 실행하는 것이 안전합니다.

## ID 범위

- `university`: `1001` ~ `1030`
- `terms`: `1101` ~ `1103`
- `member`: `1000001` ~ `1050000`
- `major.board`: `2100001` ~ `2100300`
- `major`: `3100001` ~ `3100300`
- `interest.board`: `2200001` ~ `2200200`
- `interest`: `3200001` ~ `3200200`
- `post.board`: `2000001` ~ `2000120`
- `post`: `3000001` ~ `5000000`
- `popular.board`: `2001001`
- `popular.post`: `5000001` ~ `6000000`
- `05b popular.post`(추가분): `6000001` ~ `12000000`, `05b secondary.post`(board `2001002`): `12000001` ~ `14000000`
- `05d post`(일반 게시판 추가분): `14000001` ~ `30000000`
- `comment`: `6000001` ~ `9000000`
- `realistic sample`: 별도 범위 사용 (`91001` ~ `980021`)

## 보조 데이터

- `member` 스크립트는 `member_auth`, `member_profile`, `member_status_history`, `terms`, `member_terms_agreement`도 함께 생성합니다.
- `major` 스크립트는 `board`, `member_major`를 함께 생성합니다.
- `interest` 스크립트는 `board`, `interest_major_relation`, `member_interests`를 함께 생성합니다.
- `post` 스크립트는 `board`, `post_view_count`, `post_like_count`, `post_bookmark_count`, `post_tag`, `post_image`를 함께 생성합니다.
- `popular board post` 스크립트는 인기 게시판용 `board`, `post`, `post_view_count`, `post_like_count`, `post_bookmark_count`, `post_tag`, `post_image`를 추가 생성합니다.
- `05d_post_seed_30m.sql`은 `post`와 카운트 4종(`post_view_count`, `post_like_count`, `post_comment_count`, `post_bookmark_count`)만 만듭니다. `board`는 만들지 않고(05가 만든 것을 재사용), 목록·조회수 측정에 쓰이지 않는 `post_tag`/`post_image`는 넣지 않습니다.
- `05d`의 `created_at`은 다른 시드와 달리 `NOW()` 상대시간이 아니라 **2024-01-01 ~ 2026-06-30 고정 구간**에 결정적으로 흩뿌립니다(역주행 시나리오용 과거글). 커서/날짜 앵커를 이 범위 밖으로 잡지 않도록 주의하세요.
- `comment` 스크립트는 댓글 삽입 후 `post_comment_count`, `comment.reply_count`를 갱신합니다.

## 실행 환경

- MySQL 8.x
- `DELIMITER`, 저장 프로시저, `JSON_ARRAY`, `CREATE TEMPORARY TABLE` 사용
