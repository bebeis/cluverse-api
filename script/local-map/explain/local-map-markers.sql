SET @university_id = 1;
SET @campus_id = NULL;
SET @category = 'CAFE';

EXPLAIN ANALYZE
SELECT p.place_id, p.name, p.category, p.latitude, p.longitude,
       COUNT(*) AS recommendation_count, MAX(r.created_at) AS last_recommended_at
FROM (
    SELECT pp.place_id, pp.created_at
    FROM post_place pp
    JOIN post po ON po.post_id = pp.post_id
    WHERE pp.author_university_id = @university_id
      AND (@campus_id IS NULL OR pp.university_campus_id = @campus_id)
      AND pp.university_campus_id IS NOT NULL
      AND pp.recommended = TRUE
      AND po.status = 'ACTIVE'
      AND po.is_external_visible = TRUE
    UNION ALL
    SELECT cp.place_id, cp.created_at
    FROM comment_place cp
    JOIN comment co ON co.comment_id = cp.comment_id
    JOIN post po ON po.post_id = co.post_id
    WHERE cp.author_university_id = @university_id
      AND (@campus_id IS NULL OR cp.university_campus_id = @campus_id)
      AND cp.university_campus_id IS NOT NULL
      AND cp.recommended = TRUE
      AND co.status = 'ACTIVE'
      AND po.status = 'ACTIVE'
      AND po.is_external_visible = TRUE
) r
JOIN place p ON p.place_id = r.place_id
WHERE (@category IS NULL OR p.category = @category)
GROUP BY p.place_id, p.name, p.category, p.latitude, p.longitude
ORDER BY recommendation_count DESC, last_recommended_at DESC, p.place_id DESC;
