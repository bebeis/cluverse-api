-- ===========================================================================
-- v1-optimistic.sql — V1 낙관적 락 조회수 증가의 실제 쿼리 EXPLAIN
-- ===========================================================================
-- V1 은 post_view_count_optimistic 을 @Version 낙관적 락으로 갱신한다.
-- 애플리케이션이 매 요청마다 아래 2쿼리를 순서대로 날린다:
--   1) SELECT  — 현재 view_count 와 version 을 읽는다 (락 없음)
--   2) UPDATE  — WHERE 에 읽었던 version 을 포함해 갱신. 그 사이 다른 트랜잭션이
--                먼저 커밋했으면 version 불일치로 0 row → 애플리케이션이 새
--                트랜잭션으로 재시도한다 (최대 10회, 회당 10ms 대기).
--
-- 파라미터:
--   @post_id : 대상 게시글 (핫 레코드 = 5999999 (최신 글 6000000은 시드 규칙상 DELETED))
--   UPDATE 의 SET/WHERE version 값은 예시 리터럴 — 1)의 SELECT 결과로 바꿔 실험한다.
--
-- [확인 포인트]
--   * SELECT/UPDATE 모두 PRIMARY 키 단건 접근 (type=const/range, rows=1)
--   * 플랜 자체는 V2/V3 와 차이가 없다. V1 의 비용은 플랜이 아니라
--     "경합 시 재시도 횟수 × (왕복 2쿼리 + 10ms 대기)" 로 나타난다.
--   * 부하 중 재시도가 얼마나 발생하는지는 k6 의 view_count_retry_exhausted(소진율)와
--     지연 분포의 10ms 계단(p95/p99), verify-integrity.sql 의 version 증가량으로 관찰.
-- ---------------------------------------------------------------------------
SET @post_id = 5999999;

EXPLAIN
SELECT post_id, view_count, version
FROM post_view_count_optimistic
WHERE post_id = @post_id;

-- version 리터럴(0)은 예시값 — 위 SELECT 로 읽은 version 으로 교체해서 실험한다.
EXPLAIN
UPDATE post_view_count_optimistic
SET view_count = view_count + 1,
    version = version + 1,
    updated_at = NOW()
WHERE post_id = @post_id
  AND version = 0;
