EXPLAIN
SELECT c.post_id, MAX(c.visible_created_at) AS last_commented_at
FROM comment c
GROUP BY c.post_id
HAVING MAX(c.visible_created_at) IS NOT NULL
ORDER BY last_commented_at DESC, c.post_id DESC
LIMIT 201;

EXPLAIN ANALYZE
SELECT c.post_id, MAX(c.visible_created_at) AS last_commented_at
FROM comment c
GROUP BY c.post_id
HAVING MAX(c.visible_created_at) IS NOT NULL
ORDER BY last_commented_at DESC, c.post_id DESC
LIMIT 201;
