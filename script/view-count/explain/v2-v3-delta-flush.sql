SET @post_id = 5999999;
SET @delta = 100;
EXPLAIN ANALYZE
UPDATE post_view_count
SET view_count = view_count + @delta, updated_at = NOW()
WHERE post_id = @post_id;
