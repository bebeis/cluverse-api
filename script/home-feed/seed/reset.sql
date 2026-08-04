START TRANSACTION;

DELETE activity
FROM post_comment_activity activity
JOIN post p ON p.post_id = activity.post_id
WHERE p.client_ip = 'benchmark-home-feed';

DELETE comment_like
FROM comment_like
JOIN comment c ON c.comment_id = comment_like.comment_id
JOIN post p ON p.post_id = c.post_id
WHERE p.client_ip = 'benchmark-home-feed';

DELETE comment_place
FROM comment_place
JOIN comment c ON c.comment_id = comment_place.comment_id
JOIN post p ON p.post_id = c.post_id
WHERE p.client_ip = 'benchmark-home-feed';

DELETE c
FROM comment c
JOIN post p ON p.post_id = c.post_id
WHERE p.client_ip = 'benchmark-home-feed';

DELETE post_comment_count
FROM post_comment_count
JOIN post p ON p.post_id = post_comment_count.post_id
WHERE p.client_ip = 'benchmark-home-feed';

DELETE FROM post
WHERE client_ip = 'benchmark-home-feed';

COMMIT;

SELECT COUNT(*) AS remaining_benchmark_posts
FROM post
WHERE client_ip = 'benchmark-home-feed';
