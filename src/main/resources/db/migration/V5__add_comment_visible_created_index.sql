ALTER TABLE comment
    ADD COLUMN visible_created_at DATETIME
        GENERATED ALWAYS AS (
            CASE
                WHEN status <> 'DELETED' THEN created_at
                ELSE NULL
            END
        ) STORED,
    ADD INDEX idx_comment_post_visible_created (post_id, visible_created_at);
