-- 최근 인기글: 아직 48시간 최종화되지 않은 승격 결과를 승격 최신순으로 조회.
SET @algorithm_version = 'V2';
SET @size = 20;

EXPLAIN
SELECT pp.post_id,
       pp.board_id,
       p.title,
       pp.promoted_at,
       COALESCE(pp.score, pp.score_at_promotion) AS score
FROM popular_post pp
JOIN post p ON p.post_id = pp.post_id
WHERE pp.algorithm_version = @algorithm_version
  AND pp.finalized_at IS NULL
  AND p.status = 'ACTIVE'
ORDER BY pp.promoted_at DESC, pp.post_id DESC
LIMIT 20;

EXPLAIN ANALYZE
SELECT pp.post_id,
       pp.board_id,
       p.title,
       pp.promoted_at,
       COALESCE(pp.score, pp.score_at_promotion) AS score
FROM popular_post pp
JOIN post p ON p.post_id = pp.post_id
WHERE pp.algorithm_version = @algorithm_version
  AND pp.finalized_at IS NULL
  AND p.status = 'ACTIVE'
ORDER BY pp.promoted_at DESC, pp.post_id DESC
LIMIT 20;

-- 확인: (algorithm_version, finalized_at, promoted_at, post_id) 인덱스로 LIMIT만큼만 읽는지.
