SET @benchmark_post_id = COALESCE(@benchmark_post_id, 1);
SET @benchmark_run_id = COALESCE(@benchmark_run_id, '');

START TRANSACTION;

DELETE FROM comment
WHERE post_id = @benchmark_post_id
  AND (
    client_ip = 'benchmark-comment-pagination'
    OR (
      @benchmark_run_id <> ''
      AND content IN (
        CONCAT('comment-pagination-benchmark:', @benchmark_run_id, ':root'),
        CONCAT('comment-pagination-benchmark:', @benchmark_run_id, ':reply')
      )
    )
  );

UPDATE comment parent
LEFT JOIN (
    SELECT parent_id, COUNT(*) AS child_count
    FROM comment
    WHERE post_id = @benchmark_post_id
      AND parent_id IS NOT NULL
    GROUP BY parent_id
) children ON children.parent_id = parent.comment_id
SET parent.reply_count = COALESCE(children.child_count, 0)
WHERE parent.post_id = @benchmark_post_id;

INSERT INTO post_comment_count (post_id, comment_count)
SELECT @benchmark_post_id, COUNT(*)
FROM comment
WHERE post_id = @benchmark_post_id
ON DUPLICATE KEY UPDATE comment_count = VALUES(comment_count);

COMMIT;

SELECT COUNT(*) AS remaining_benchmark_comments
FROM comment
WHERE post_id = @benchmark_post_id
  AND (
    client_ip = 'benchmark-comment-pagination'
    OR (
      @benchmark_run_id <> ''
      AND content IN (
        CONCAT('comment-pagination-benchmark:', @benchmark_run_id, ':root'),
        CONCAT('comment-pagination-benchmark:', @benchmark_run_id, ':reply')
      )
    )
  );
