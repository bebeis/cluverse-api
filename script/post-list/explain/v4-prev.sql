-- ===========================================================================
-- v4-prev.sql — V4 PREV(이전 페이지) 커서 이동 ID 슬라이스의 EXPLAIN
-- ===========================================================================
-- PREV 는 NEXT 의 거울상이다. 부등호 방향과 정렬을 모두 반전해서(ASC) 커서 "앞쪽"
-- size+1 행을 range 스캔한 뒤, 애플리케이션에서 다시 뒤집어(DESC) 화면 순서를 맞춘다.
--   · NEXT: created_at < c … ORDER BY created_at DESC, post_id DESC
--   · PREV: created_at > c … ORDER BY created_at ASC,  post_id ASC   ← 반전
--
-- 파라미터:
--   @board_id           : 대상 게시판 (핫보드 2001001)
--   @cursor_created_at  : 현재 페이지 첫 글의 created_at
--   @cursor_post_id     : 현재 페이지 첫 글의 post_id (동률 tie-break)
--   size+1              : hasPrev 판정용 (size=20 → 21) — LIMIT 리터럴
-- ---------------------------------------------------------------------------
SET @board_id          = 2001001;
SET @cursor_created_at = '2024-06-01 12:34:56';
SET @cursor_post_id    = 5500000;

-- [확인 포인트]
--   * type = range, key = idx_post_board_status_created_id, Extra 에 "Using index"
--   * 정렬이 인덱스와 반대 방향(ASC)이라도 InnoDB 는 인덱스 역방향 스캔(Backward
--     index scan)으로 처리 가능 — Extra 에 "Backward index scan" 표시 여부 확인
--   * actual rows 가 커서 깊이와 무관하게 ~21 (NEXT 와 동일하게 O(1))

EXPLAIN
SELECT p.post_id
FROM post p
WHERE p.board_id = @board_id
  AND p.status = 'ACTIVE'
  AND (
        p.created_at > @cursor_created_at
     OR (p.created_at = @cursor_created_at AND p.post_id > @cursor_post_id)
      )
ORDER BY p.created_at ASC, p.post_id ASC
LIMIT 21;

EXPLAIN ANALYZE
SELECT p.post_id
FROM post p
WHERE p.board_id = @board_id
  AND p.status = 'ACTIVE'
  AND (
        p.created_at > @cursor_created_at
     OR (p.created_at = @cursor_created_at AND p.post_id > @cursor_post_id)
      )
ORDER BY p.created_at ASC, p.post_id ASC
LIMIT 21;
