SET @viewer_id = COALESCE(@viewer_id, 1);

EXPLAIN ANALYZE
SELECT p.post_id, p.title, MAX(c.created_at) AS last_commented_at
FROM comment c
JOIN post p ON p.post_id = c.post_id
JOIN board b ON b.board_id = p.board_id
JOIN member author ON author.member_id = p.member_id
WHERE c.status <> 'DELETED'
  AND p.status = 'ACTIVE'
  AND b.is_active = TRUE
  AND author.status = 'ACTIVE'
  AND NOT EXISTS (
        SELECT 1
        FROM block bl
        WHERE bl.blocker_id = @viewer_id
          AND bl.blocked_id = p.member_id
  )
  AND (b.board_type <> 'GROUP' OR b.board_id IN (-1))
GROUP BY p.post_id, p.title
ORDER BY last_commented_at DESC, p.post_id DESC
LIMIT 10;
