SET @post_id = 5999999;
EXPLAIN ANALYZE
SELECT p.post_id, p.board_id, p.created_at,
       COALESCE(l.like_count, 0) AS like_count,
       COALESCE(c.comment_count, 0) AS comment_count
FROM post p
LEFT JOIN post_like_count l ON l.post_id = p.post_id
LEFT JOIN post_comment_count c ON c.post_id = p.post_id
WHERE p.post_id = @post_id
  AND p.status = 'ACTIVE';
