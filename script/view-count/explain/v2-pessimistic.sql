-- ===========================================================================
-- v2-pessimistic.sql — V2 비관적 락 조회수 증가의 실제 쿼리 EXPLAIN
-- ===========================================================================
-- V2 는 post_view_count 레코드를 SELECT ... FOR UPDATE 로 배타 락을 잡고 읽은 뒤,
-- JPA 더티체킹이 만드는 UPDATE 를 커밋 시점에 발행한다.
--   1) SELECT ... FOR UPDATE — 레코드 락 획득 (트랜잭션 커밋까지 보유)
--   2) UPDATE               — 더티체킹 결과. 절대값 대입(SET view_count = ?)
--
-- 파라미터:
--   @post_id : 대상 게시글 (핫 레코드 = 05a 시드 최신 글 6000000)
--
-- [확인 포인트]
--   * 두 쿼리 모두 PRIMARY 키 단건 접근 — 플랜은 V3(원자적 UPDATE)와 동일하다.
--   * 즉 V2 와 V3 의 차이는 플랜이 아니라 "락 보유 시간"이다.
--     - V2: SELECT FOR UPDATE 시점 ~ 트랜잭션 커밋까지 락 보유
--            (그 사이 애플리케이션 왕복 + 커밋 fsync 가 전부 락 구간에 포함)
--     - V3: UPDATE 문장 하나가 실행되는 짧은 순간만 락 보유
--   * 락 보유 시간의 차이는 EXPLAIN 이 아니라 부하 중 lock-waits.sql 스냅샷
--     (data_lock_waits 행 수, Innodb_row_lock_time 델타)로 관찰한다.
-- ---------------------------------------------------------------------------
SET @post_id = 6000000;

EXPLAIN
SELECT post_id, view_count
FROM post_view_count
WHERE post_id = @post_id
FOR UPDATE;

-- 더티체킹이 발행하는 UPDATE (절대값 대입). view_count 리터럴은 예시값.
EXPLAIN
UPDATE post_view_count
SET view_count = 100001,
    updated_at = NOW()
WHERE post_id = @post_id;
