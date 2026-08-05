CREATE TABLE post_image_upload (
    post_image_upload_id BIGINT NOT NULL AUTO_INCREMENT,
    request_id CHAR(36) NOT NULL,
    version VARCHAR(8) NOT NULL,
    status VARCHAR(16) NOT NULL,
    failure_reason VARCHAR(500) NULL,
    staging_cleaned TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (post_image_upload_id),
    UNIQUE KEY uk_post_image_upload_version_request (version, request_id),
    KEY idx_post_image_upload_status_updated (status, updated_at)
);

CREATE TABLE post_image_asset (
    post_image_asset_id BIGINT NOT NULL AUTO_INCREMENT,
    post_image_upload_id BIGINT NOT NULL,
    display_order INT NOT NULL,
    staging_key VARCHAR(255) NULL,
    content_key VARCHAR(255) NOT NULL,
    thumbnail_key VARCHAR(255) NULL,
    source_bytes BIGINT NOT NULL,
    content_type VARCHAR(100) NULL,
    content_width INT NULL,
    content_height INT NULL,
    content_bytes BIGINT NULL,
    thumbnail_width INT NULL,
    thumbnail_height INT NULL,
    thumbnail_bytes BIGINT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (post_image_asset_id),
    UNIQUE KEY uk_post_image_asset_upload_order (post_image_upload_id, display_order),
    CONSTRAINT fk_post_image_asset_upload
        FOREIGN KEY (post_image_upload_id)
        REFERENCES post_image_upload (post_image_upload_id)
        ON DELETE CASCADE
);
