SET @board_id = 2001001;
SET @sample_start = NOW() - INTERVAL 9 DAY;
SET @sample_end = NOW() - INTERVAL 2 DAY;
EXPLAIN ANALYZE
SELECT pp.score_at_promotion, COALESCE(l.like_count, 0), COALESCE(c.comment_count, 0)
FROM post p
LEFT JOIN post_like_count l ON l.post_id = p.post_id
LEFT JOIN post_comment_count c ON c.post_id = p.post_id
LEFT JOIN popular_post pp ON pp.post_id = p.post_id AND pp.algorithm_version = 'V2'
WHERE p.board_id = @board_id
  AND p.created_at >= @sample_start
  AND p.created_at < @sample_end
  AND p.status = 'ACTIVE';
