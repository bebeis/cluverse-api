SET @benchmark_post_id = COALESCE(@benchmark_post_id, 1);

START TRANSACTION;

DELETE FROM comment
WHERE post_id = @benchmark_post_id
  AND client_ip = 'benchmark-comment-pagination';

INSERT INTO post_comment_count (post_id, comment_count)
SELECT @benchmark_post_id, COUNT(*)
FROM comment
WHERE post_id = @benchmark_post_id
ON DUPLICATE KEY UPDATE comment_count = VALUES(comment_count);

COMMIT;

SELECT COUNT(*) AS remaining_benchmark_comments
FROM comment
WHERE post_id = @benchmark_post_id
  AND client_ip = 'benchmark-comment-pagination';
