-- ===========================================================================
-- v1-count.sql — V1 전체 COUNT 쿼리의 EXPLAIN
-- ===========================================================================
-- V1 은 페이지네이션 메타(총 페이지 수)를 위해 board 전체 활성 게시글을 COUNT 한다.
-- 게시글이 100만+ 인 핫보드에서는 이 COUNT 자체가 목록 쿼리만큼 비싸다.
--
-- 파라미터:
--   @board_id : 대상 게시판 (핫보드 2001001)
-- ---------------------------------------------------------------------------
SET @board_id = 2001001;

-- [확인 포인트]
--   * idx_post_board_status_created_id 로 커버되지만(Using index),
--     매칭 행 전체(100만+)를 세므로 rows 가 board 활성글 총량에 비례.
--   * 이 "전체 COUNT" 를 V3 에서 상한 카운트(v3-count.sql)로 대체하는 게 개선 서사.

EXPLAIN
SELECT COUNT(*)
FROM post p
WHERE p.board_id = @board_id
  AND p.status = 'ACTIVE';

EXPLAIN ANALYZE
SELECT COUNT(*)
FROM post p
WHERE p.board_id = @board_id
  AND p.status = 'ACTIVE';
