SET @post_id = COALESCE(@post_id, 1);
SET @as_of = COALESCE(@as_of, CURRENT_TIMESTAMP);
SET @snapshot_max_comment_id = COALESCE(
    @snapshot_max_comment_id,
    (SELECT COALESCE(MAX(comment_id), 0) FROM comment)
);
SET @cursor_path = COALESCE(@cursor_path, '');

EXPLAIN ANALYZE
SELECT comment_id, path
FROM comment
WHERE post_id = @post_id
  AND created_at <= @as_of
  AND comment_id <= @snapshot_max_comment_id
  AND (@cursor_path = '' OR path > @cursor_path)
ORDER BY path
LIMIT 101;
