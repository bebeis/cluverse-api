-- 과거 인기글 점수순. 동점은 post_id로 안정적으로 정렬한다.
SET @algorithm_version = 'V2';
SET @size = 20;

EXPLAIN
SELECT pp.post_id,
       pp.board_id,
       p.title,
       pp.finalized_at,
       pp.score,
       pp.like_count,
       pp.comment_count,
       pp.view_count
FROM popular_post pp
JOIN post p ON p.post_id = pp.post_id
WHERE pp.algorithm_version = @algorithm_version
  AND pp.finalized_at IS NOT NULL
  AND p.status = 'ACTIVE'
ORDER BY pp.score DESC, pp.post_id DESC
LIMIT 20;

EXPLAIN ANALYZE
SELECT pp.post_id,
       pp.board_id,
       p.title,
       pp.finalized_at,
       pp.score,
       pp.like_count,
       pp.comment_count,
       pp.view_count
FROM popular_post pp
JOIN post p ON p.post_id = pp.post_id
WHERE pp.algorithm_version = @algorithm_version
  AND pp.finalized_at IS NOT NULL
  AND p.status = 'ACTIVE'
ORDER BY pp.score DESC, pp.post_id DESC
LIMIT 20;

-- 확인: 랭킹 인덱스 사용, filesort 여부, LIMIT 20을 만들기 위해 읽은 actual rows.
