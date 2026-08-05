DROP TABLE IF EXISTS popularity_candidate;
DROP TABLE IF EXISTS popularity_finalization_claim;

ALTER TABLE popular_post
    DROP COLUMN view_count,
    DROP COLUMN like_gate_threshold,
    DROP COLUMN comment_gate_threshold;

ALTER TABLE board_popularity_policy
    DROP COLUMN like_gate,
    DROP COLUMN comment_gate;
