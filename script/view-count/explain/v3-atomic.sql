-- ===========================================================================
-- v3-atomic.sql — V3 원자적 UPDATE 조회수 증가의 실제 쿼리 EXPLAIN
-- ===========================================================================
-- V3 는 읽기 없이 UPDATE 한 문장으로 증가시킨다 (운영 방식).
-- DB 가 문장 실행 순간에만 레코드 락을 잡으므로 락 보유 시간이 가장 짧다.
--
-- 파라미터:
--   @post_id : 대상 게시글 (핫 레코드 = 05a 시드 최신 글 6000000)
--
-- [확인 포인트]
--   * PRIMARY 키 단건 접근 (rows=1) — 플랜은 V2 의 UPDATE 와 동일하다.
--   * 차이는 락 보유 시간: SELECT 왕복 없이 문장 단위 락으로 끝난다.
--     부하 중 lock-waits.sql 에서 V2 대비 data_lock_waits 가 거의 잡히지 않고
--     Innodb_row_lock_time 델타가 작은 것으로 확인한다.
--   * MySQL 8 의 EXPLAIN ANALYZE 는 단일 테이블 UPDATE 를 지원하지 않으므로
--     EXPLAIN(+ FORMAT=TREE)까지만 확인한다.
-- ---------------------------------------------------------------------------
SET @post_id = 6000000;

EXPLAIN
UPDATE post_view_count
SET view_count = view_count + 1,
    updated_at = NOW()
WHERE post_id = @post_id;

EXPLAIN FORMAT=TREE
UPDATE post_view_count
SET view_count = view_count + 1,
    updated_at = NOW()
WHERE post_id = @post_id;
