-- ===========================================================================
-- v4-entry.sql — V4 진입(무앵커 최신 / 날짜 앵커) ID 슬라이스의 EXPLAIN
-- ===========================================================================
-- V4 는 offset 을 완전히 버리고, 인덱스 정렬 순서(created_at DESC, post_id DESC)를
-- 따라 "커서 위치부터 size+1 행" 만 range 스캔한다. 진입은 커서가 아직 없으므로
-- 앵커(날짜) 또는 최신부터 시작한다.
--
-- 파라미터:
--   @board_id    : 대상 게시판 (핫보드 2001001)
--   @anchor_end  : 날짜 앵커 상한(exclusive). date=yyyy-MM-dd 로 진입하면
--                  해당 날짜 다음날 00:00:00 을 넣어 "그 날짜 이하" 최신부터 시작.
--                  무앵커 최신 진입이면 이 조건을 빼고 board+status 만으로 시작한다.
--   size+1       : hasNext 판정용 (size=20 → 21) — LIMIT 리터럴
--
-- ※ MySQL 은 LIMIT 에 사용자 변수를 못 쓰므로 리터럴 21 로 둔다(v1-list.sql 참고).
-- ---------------------------------------------------------------------------
SET @board_id   = 2001001;
SET @anchor_end = '2024-06-02 00:00:00'; -- date=2024-06-01 진입 → 다음날 00:00

-- [확인 포인트]
--   * type = range, key = idx_post_board_status_created_id
--   * Extra = "Using index" (post_id 만 뽑으므로 커버링, 테이블 접근 0)
--   * created_at < @anchor_end 가 인덱스 range 시작점으로 쓰여 앞부분을 건너뛴다
--     (offset 처럼 "읽고 버리는" 게 아니라 인덱스 탐색으로 바로 시작 위치로 점프)

-- (A) 날짜 앵커 진입
EXPLAIN
SELECT p.post_id
FROM post p
WHERE p.board_id = @board_id
  AND p.status = 'ACTIVE'
  AND p.created_at < @anchor_end
ORDER BY p.created_at DESC, p.post_id DESC
LIMIT 21;

EXPLAIN ANALYZE
SELECT p.post_id
FROM post p
WHERE p.board_id = @board_id
  AND p.status = 'ACTIVE'
  AND p.created_at < @anchor_end
ORDER BY p.created_at DESC, p.post_id DESC
LIMIT 21;

-- (B) 무앵커 최신 진입 (참고: created_at 조건 없이 인덱스 선두부터)
EXPLAIN ANALYZE
SELECT p.post_id
FROM post p
WHERE p.board_id = @board_id
  AND p.status = 'ACTIVE'
ORDER BY p.created_at DESC, p.post_id DESC
LIMIT 21;
