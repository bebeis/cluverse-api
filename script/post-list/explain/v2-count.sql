-- ===========================================================================
-- v2-count.sql — V2 전체 COUNT 쿼리의 EXPLAIN
-- ===========================================================================
-- V2 는 프로젝션을 deferred join 으로 개선했지만, 총 페이지 수를 위한 COUNT 는
-- 여전히 board 전체를 센다 → v1-count.sql 과 완전히 동일한 쿼리/비용.
--
-- 즉 V2 의 남은 병목이 바로 이 "전체 COUNT" 이며, 이것을 상한 카운트로 바꾼 것이
-- V3 다 (v3-count.sql 참고).
--
-- 파라미터:
--   @board_id : 대상 게시판 (핫보드 2001001)
-- ---------------------------------------------------------------------------
SET @board_id = 2001001;

-- [확인 포인트] v1-count.sql 과 동일 — Using index 이지만 매칭 행 전체(100만+)를 센다.

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
