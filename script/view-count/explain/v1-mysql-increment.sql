SET @post_id = 5999999;
EXPLAIN ANALYZE
UPDATE post_view_count
SET view_count = view_count + 1, updated_at = NOW()
WHERE post_id = @post_id;
