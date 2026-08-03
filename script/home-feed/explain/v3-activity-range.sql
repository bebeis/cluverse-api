SET @viewer_id = COALESCE(@viewer_id, 1);

EXPLAIN ANALYZE
SELECT /*+ JOIN_ORDER(activity, p, b, author) NO_BNL(p, b, author) */
       p.post_id, p.title, activity.last_commented_at
FROM post_comment_activity activity
JOIN post p ON p.post_id = activity.post_id
JOIN board b ON b.board_id = p.board_id
JOIN member author ON author.member_id = p.member_id
WHERE p.status = 'ACTIVE'
  AND b.is_active = TRUE
  AND author.status = 'ACTIVE'
  AND NOT EXISTS (
        SELECT 1
        FROM block bl
        WHERE bl.blocker_id = @viewer_id
          AND bl.blocked_id = p.member_id
  )
  AND (b.board_type <> 'GROUP' OR b.board_id IN (-1))
ORDER BY activity.last_commented_at DESC, activity.post_id DESC
LIMIT 10;
