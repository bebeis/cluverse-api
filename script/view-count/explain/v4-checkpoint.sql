SET @post_id = 5999999;
SET @snapshot = 10000;
EXPLAIN ANALYZE
UPDATE post_view_count
SET view_count = GREATEST(view_count, @snapshot), updated_at = NOW()
WHERE post_id = @post_id;
