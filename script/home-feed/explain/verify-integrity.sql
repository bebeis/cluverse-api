WITH ranked_comment AS (
    SELECT c.post_id,
           c.comment_id,
           c.created_at,
           ROW_NUMBER() OVER (
               PARTITION BY c.post_id
               ORDER BY c.created_at DESC, c.comment_id DESC
           ) AS row_num
    FROM comment c
    WHERE c.status <> 'DELETED'
), expected AS (
    SELECT post_id, comment_id, created_at
    FROM ranked_comment
    WHERE row_num = 1
)
SELECT COUNT(*) AS mismatch_count
FROM (
    SELECT expected.post_id
    FROM expected
    LEFT JOIN post_comment_activity activity ON activity.post_id = expected.post_id
    WHERE activity.post_id IS NULL
       OR activity.last_comment_id <> expected.comment_id
       OR activity.last_commented_at <> expected.created_at
    UNION ALL
    SELECT activity.post_id
    FROM post_comment_activity activity
    LEFT JOIN expected ON expected.post_id = activity.post_id
    WHERE expected.post_id IS NULL
) mismatch;
