-- ===========================================================================
-- v4-next.sql — V4 NEXT(다음 페이지) 커서 이동 ID 슬라이스의 EXPLAIN
-- ===========================================================================
-- 커서는 직전 페이지 마지막 글의 (created_at, post_id) 튜플이다. 그 지점 "이후"의
-- size+1 행을 range 스캔한다. 몇 번째 이동이든 스캔 폭이 일정 = O(1) 이 관전 포인트.
--
-- 튜플 비교(created_at < c) OR (created_at = c AND post_id < p) 는 복합 인덱스
-- (…, created_at DESC, post_id DESC) 위에서 단일 range 시작점으로 최적화된다.
--
-- 파라미터:
--   @board_id           : 대상 게시판 (핫보드 2001001)
--   @cursor_created_at  : 직전 페이지 마지막 글의 created_at
--   @cursor_post_id     : 직전 페이지 마지막 글의 post_id (created_at 동률 tie-break)
--   size+1              : hasNext 판정용 (size=20 → 21) — LIMIT 리터럴
-- ---------------------------------------------------------------------------
SET @board_id          = 2001001;
SET @cursor_created_at = '2024-06-01 12:34:56';
SET @cursor_post_id    = 5500000;

-- [확인 포인트]
--   * type = range, key = idx_post_board_status_created_id, Extra = "Using index"
--   * 튜플 부등호가 range 조건으로 인식되어 커서 지점부터 21행만 읽는다
--     (EXPLAIN ANALYZE 의 actual rows 가 커서 깊이와 무관하게 ~21)
--   * created_at 이 동률인 경계에서 post_id 로 정확히 이어붙는지(중복/누락 없음) 확인

EXPLAIN
SELECT p.post_id
FROM post p
WHERE p.board_id = @board_id
  AND p.status = 'ACTIVE'
  AND (
        p.created_at < @cursor_created_at
     OR (p.created_at = @cursor_created_at AND p.post_id < @cursor_post_id)
      )
ORDER BY p.created_at DESC, p.post_id DESC
LIMIT 21;

EXPLAIN ANALYZE
SELECT p.post_id
FROM post p
WHERE p.board_id = @board_id
  AND p.status = 'ACTIVE'
  AND (
        p.created_at < @cursor_created_at
     OR (p.created_at = @cursor_created_at AND p.post_id < @cursor_post_id)
      )
ORDER BY p.created_at DESC, p.post_id DESC
LIMIT 21;
