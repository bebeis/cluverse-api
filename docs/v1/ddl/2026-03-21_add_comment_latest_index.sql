CREATE INDEX idx_comment_post_created_latest
    ON comment (post_id, created_at DESC, comment_id DESC);
