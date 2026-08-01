-- 최종화 대상 조회는 post_id 단위로 배치 경계를 잡은 뒤, 해당 ID의 V1/V2 행을 모두 읽는다.
-- 첫 쿼리의 LIMIT이 같은 post_id의 버전 행 사이를 가르는 문제를 피하기 위한 2단계 조회다.
SET @now = NOW();

-- [1] 만기된 고유 post_id를 제한 배치로 선택한다.
EXPLAIN
SELECT DISTINCT pp.post_id
FROM popular_post pp
WHERE pp.finalized_at IS NULL
  AND pp.finalize_at <= @now
ORDER BY pp.post_id
LIMIT 500;

EXPLAIN ANALYZE
SELECT DISTINCT pp.post_id
FROM popular_post pp
WHERE pp.finalized_at IS NULL
  AND pp.finalize_at <= @now
ORDER BY pp.post_id
LIMIT 500;

-- [2] [1]에서 반환된 ID 전체에 대해 아직 만기 상태인 모든 알고리즘 행을 읽는다.
-- 아래 ID는 측정 DB에서 [1]의 실제 결과 일부로 바꾼다.
EXPLAIN
SELECT pp.*
FROM popular_post pp
WHERE pp.post_id IN (9100000001, 9100000002, 9100000003)
  AND pp.finalized_at IS NULL
  AND pp.finalize_at <= @now
ORDER BY pp.post_id, pp.algorithm_version;

EXPLAIN ANALYZE
SELECT pp.*
FROM popular_post pp
WHERE pp.post_id IN (9100000001, 9100000002, 9100000003)
  AND pp.finalized_at IS NULL
  AND pp.finalize_at <= @now
ORDER BY pp.post_id, pp.algorithm_version;

-- 확인:
--   * [1]의 반환 행은 algorithm row 수가 아니라 고유 post_id 수이며 500 이하여야 한다.
--   * [2]는 선택된 post_id마다 V1/V2 만기 행을 모두 반환해야 한다.
--   * 두 쿼리의 actual rows, temporary/filesort, 선택한 ID 수 증가에 따른 비용을 기록한다.
--   * [1]은 idx_popular_finalize_due, [2]는 idx_popular_finalize_post 사용 여부를 확인한다.
