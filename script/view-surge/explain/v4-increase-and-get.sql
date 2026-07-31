-- ===========================================================================
-- v4-increase-and-get.sql — V4 평상시 경로(MySQL 원자적 UPDATE + 증가값 회수) EXPLAIN
-- ===========================================================================
-- V4 는 급상승으로 감지되기 전까지 V3 와 같은 경로로 동작한다. 다만 급상승 판정에
-- 필요한 "증가 직후의 조회수"를 알아야 해서, UPDATE 에 LAST_INSERT_ID() 를 끼워
-- 갱신된 값을 세션 변수에 실어 보낸 뒤 SELECT 한 번으로 회수한다.
-- (SELECT 를 따로 날려 읽으면 두 문장 사이에 다른 요청이 끼어들어 값이 어긋난다.)
--
-- 파라미터:
--   @post_id : 대상 게시글 (핫 레코드 = 5999999. 최신 글 6000000은 시드 규칙상 DELETED)
--
-- [확인 포인트]
--   * PRIMARY 키 단건 접근 (rows=1) — 플랜은 V3 의 UPDATE 와 완전히 동일하다.
--     즉 V4 의 상시 오버헤드는 쿼리 플랜이 아니라 애플리케이션 레벨(속도 집계)에서
--     나온다. 그 크기는 bench 의 V3 vs V4 균등 분포 비교로 잰다.
--   * LAST_INSERT_ID(expr) 는 세션 값을 덮어쓴다. UPDATE 와 SELECT 가 반드시
--     같은 커넥션에서 실행돼야 한다 — 커넥션 풀을 쓰는 앱에서는 두 문장이 같은
--     트랜잭션(=같은 커넥션) 안에 있는지 확인해야 한다. 아래 두 문장을 각각
--     다른 mysql 세션에서 실행하면 SELECT 결과가 0 이거나 남의 값이 나온다.
--   * MySQL 8 의 EXPLAIN ANALYZE 는 단일 테이블 UPDATE 를 지원하지 않으므로
--     EXPLAIN(+ FORMAT=TREE)까지만 확인한다.
-- ---------------------------------------------------------------------------
SET @post_id = 5999999;

EXPLAIN
UPDATE post_view_count
SET view_count = LAST_INSERT_ID(view_count + 1),
    updated_at = NOW()
WHERE post_id = @post_id;

EXPLAIN FORMAT=TREE
UPDATE post_view_count
SET view_count = LAST_INSERT_ID(view_count + 1),
    updated_at = NOW()
WHERE post_id = @post_id;

-- 갱신된 조회수 회수 — 위 UPDATE 와 같은 커넥션에서만 유효하다.
-- (EXPLAIN 대상이 아니다. 테이블 접근이 없는 세션 변수 읽기라 비용은 0 에 가깝다.)
SELECT LAST_INSERT_ID() AS view_count_after_increase;
