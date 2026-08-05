DROP TABLE IF EXISTS view_surge_tracking;
DROP TABLE IF EXISTS post_view_count_optimistic;

ALTER TABLE post_view_count
    MODIFY COLUMN view_count BIGINT NOT NULL DEFAULT 0;
