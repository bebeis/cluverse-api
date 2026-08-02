-- 캡처 확인점: Table scan on comment_tree의 actual rows/loops, Sort, Materialize
SET @post_id = COALESCE(@post_id, 1);
SET @as_of = COALESCE(@as_of, CURRENT_TIMESTAMP);
SET @snapshot_max_comment_id = COALESCE(
    @snapshot_max_comment_id,
    (SELECT COALESCE(MAX(comment_id), 0) FROM comment)
);
SET @cursor_path = COALESCE(@cursor_path, '');
SET @parent_path = COALESCE(
    (SELECT path FROM comment WHERE comment_id = @parent_comment_id),
    ''
);

EXPLAIN ANALYZE
WITH RECURSIVE comment_tree (comment_id, depth, sort_path) AS (
    SELECT
        c.comment_id,
        c.depth,
        CAST(
            CONCAT(
                CASE
                    WHEN @parent_comment_id IS NULL THEN ''
                    ELSE CONCAT(@parent_path, '/')
                END,
                DATE_FORMAT(c.created_at, '%Y%m%d%H%i%s'), '-',
                LPAD(RTRIM(CAST(c.comment_id AS CHAR(20))), 20, '0')
            ) AS CHAR(255)
        ) AS sort_path
    FROM comment c
    WHERE c.post_id = @post_id
      AND (
        (@parent_comment_id IS NULL AND c.parent_id IS NULL)
        OR c.parent_id = @parent_comment_id
      )
      AND c.created_at <= @as_of
      AND c.comment_id <= @snapshot_max_comment_id

    UNION ALL

    SELECT
        child.comment_id,
        child.depth,
        CONCAT(
            RTRIM(parent.sort_path), '/',
            DATE_FORMAT(child.created_at, '%Y%m%d%H%i%s'), '-',
            LPAD(RTRIM(CAST(child.comment_id AS CHAR(20))), 20, '0')
        )
    FROM comment child
    JOIN comment_tree parent ON child.parent_id = parent.comment_id
    WHERE child.post_id = @post_id
      AND child.depth <= 5
      AND child.created_at <= @as_of
      AND child.comment_id <= @snapshot_max_comment_id
)
SELECT comment_id, sort_path
FROM comment_tree
WHERE sort_path > @cursor_path
ORDER BY sort_path
LIMIT 101;
