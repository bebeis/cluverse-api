SET @place_id = 1;

EXPLAIN ANALYZE
SELECT content_type, content_id, created_at
FROM (
    SELECT 'POST' AS content_type, po.post_id AS content_id, po.created_at
    FROM post_place pp
    JOIN post po ON po.post_id = pp.post_id
    WHERE pp.place_id = @place_id
      AND po.status = 'ACTIVE'
      AND po.is_external_visible = TRUE
    UNION ALL
    SELECT 'COMMENT' AS content_type, co.comment_id AS content_id, co.created_at
    FROM comment_place cp
    JOIN comment co ON co.comment_id = cp.comment_id
    JOIN post po ON po.post_id = co.post_id
    WHERE cp.place_id = @place_id
      AND co.status = 'ACTIVE'
      AND po.status = 'ACTIVE'
      AND po.is_external_visible = TRUE
) contents
ORDER BY created_at DESC, content_type DESC, content_id DESC
LIMIT 21;
