SET @created_from = NOW() - INTERVAL 48 HOUR;
SET @last_created_at = @created_from;
SET @last_post_id = 0;
EXPLAIN ANALYZE
SELECT p.post_id, p.board_id, p.created_at,
       COALESCE(l.like_count, 0) AS like_count,
       COALESCE(c.comment_count, 0) AS comment_count
FROM post p
LEFT JOIN post_like_count l ON l.post_id = p.post_id
LEFT JOIN post_comment_count c ON c.post_id = p.post_id
WHERE p.status = 'ACTIVE'
  AND p.created_at >= @created_from
  AND (p.created_at > @last_created_at OR (p.created_at = @last_created_at AND p.post_id > @last_post_id))
ORDER BY p.created_at, p.post_id
LIMIT 1000;
