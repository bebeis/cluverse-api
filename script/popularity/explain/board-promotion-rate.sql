SELECT p.board_id,
       COUNT(*) AS eligible_posts,
       SUM(pp.popular_post_id IS NOT NULL) AS promoted_posts,
       ROUND(SUM(pp.popular_post_id IS NOT NULL) / COUNT(*) * 100, 2) AS promotion_rate_percent,
       MAX(bp.promotion_score) AS board_threshold
FROM post p
LEFT JOIN popular_post pp
  ON pp.post_id = p.post_id AND pp.algorithm_version = 'V2'
LEFT JOIN board_popularity_policy bp ON bp.board_id = p.board_id
WHERE p.created_at >= NOW() - INTERVAL 7 DAY
  AND p.status = 'ACTIVE'
GROUP BY p.board_id
HAVING COUNT(*) >= 100
ORDER BY eligible_posts DESC;
