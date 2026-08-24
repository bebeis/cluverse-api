CREATE TABLE post_place_verification (
    post_id        BIGINT       NOT NULL,
    status         VARCHAR(16)  NOT NULL,
    failure_reason VARCHAR(500) NULL,
    created_at     DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at     DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (post_id),
    KEY idx_post_place_verification_status_updated (status, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='게시글 장소 비동기 검증 상태';
