ALTER TABLE post_image_upload
    ADD COLUMN member_id BIGINT NULL AFTER request_id,
    ADD COLUMN claimed_post_id BIGINT NULL AFTER status,
    ADD COLUMN claimed_at DATETIME(6) NULL AFTER claimed_post_id;

CREATE INDEX idx_post_image_upload_member_request
    ON post_image_upload (member_id, request_id);

CREATE INDEX idx_post_image_upload_unclaimed
    ON post_image_upload (status, claimed_post_id, updated_at);

ALTER TABLE post_image
    MODIFY image_url VARCHAR(500) NULL,
    ADD COLUMN content_key VARCHAR(500) NULL AFTER image_url,
    ADD COLUMN thumbnail_key VARCHAR(500) NULL AFTER content_key;

CREATE INDEX idx_post_image_content_key
    ON post_image (content_key);
