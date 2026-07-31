-- ===========================================================================
-- v4-flush-update.sql — Redis 누적분을 MySQL 로 되쓰는 flush UPDATE EXPLAIN
-- ===========================================================================
-- 급상승 게시글의 증가분은 Redis(view:pending:{postId})에 쌓이고, flush 워커가
-- 주기적으로(기본 3초) 누적 델타를 걷어 MySQL 에 한 번에 더한다.
-- 요청 N 건이 UPDATE 1 건으로 접히는 것이 V4 의 이득이므로, 이 쿼리의 플랜보다
-- "얼마나 접혔는가"(view_surge_flush_batch_size)가 더 중요한 지표다.
--
-- 파라미터:
--   @post_id : flush 대상 게시글
--   @delta   : Redis 에서 걷어온 누적 증가분
--              (Lua 스크립트로 "읽고 0으로 초기화"를 원자 실행해 회수한 값)
--
-- [확인 포인트]
--   * PRIMARY 키 단건 접근 (rows=1) — 플랜은 V3 의 UPDATE 와 동일하다.
--     차이는 호출 빈도다. V3 는 요청당 1회, V4 는 게시글당 3초에 1회.
--   * 실제로는 급상승 게시글 여러 건 분을 JDBC batchUpdate 로 묶어 보낸다
--     (JDBC URL 의 rewriteBatchedStatements=true 로 왕복도 합쳐진다).
--     아래 EXPLAIN 은 그 배치 안의 문장 하나에 해당한다.
--   * 증가분을 `view_count = view_count + @delta` 로 더한다. 읽어서 계산한 값을
--     쓰는 게 아니므로, flush 도중 다른 경로의 증가가 끼어들어도 유실되지 않는다.
--   * flush 소요 시간(view_surge_flush_duration_seconds)이 flush 주기(3초)에
--     근접하면 워커가 밀리기 시작한다는 뜻이다. 급상승 게시글이 많을 때의
--     배치 크기와 함께 관찰한다.
-- ---------------------------------------------------------------------------
SET @post_id = 5999999;
SET @delta = 1000;

EXPLAIN
UPDATE post_view_count
SET view_count = view_count + @delta,
    updated_at = NOW()
WHERE post_id = @post_id;

EXPLAIN FORMAT=TREE
UPDATE post_view_count
SET view_count = view_count + @delta,
    updated_at = NOW()
WHERE post_id = @post_id;
