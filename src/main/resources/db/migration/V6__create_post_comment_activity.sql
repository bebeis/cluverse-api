CREATE TABLE post_comment_activity (
    post_id           BIGINT   NOT NULL,
    last_comment_id   BIGINT   NOT NULL,
    last_commented_at DATETIME NOT NULL,
    created_at        DATETIME NOT NULL DEFAULT NOW(),
    updated_at        DATETIME NOT NULL DEFAULT NOW() ON UPDATE NOW(),
    PRIMARY KEY (post_id),
    KEY idx_post_comment_activity_latest (last_commented_at DESC, post_id DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='홈 최근 댓글 글 조회용 게시글별 최신 댓글 투영';

INSERT INTO post_comment_activity (post_id, last_comment_id, last_commented_at)
SELECT ranked.post_id, ranked.comment_id, ranked.created_at
FROM (
    SELECT c.post_id,
           c.comment_id,
           c.created_at,
           ROW_NUMBER() OVER (
               PARTITION BY c.post_id
               ORDER BY c.created_at DESC, c.comment_id DESC
           ) AS row_num
    FROM comment c
    WHERE c.status <> 'DELETED'
) ranked
WHERE ranked.row_num = 1;
