ALTER TABLE comment
    MODIFY created_at DATETIME(6) NOT NULL,
    MODIFY updated_at DATETIME(6) NOT NULL;

ALTER TABLE post_comment_activity
    MODIFY last_commented_at DATETIME(6) NOT NULL;

UPDATE post_comment_activity activity
JOIN comment latest_comment
  ON latest_comment.comment_id = activity.last_comment_id
SET activity.last_commented_at = latest_comment.created_at;
