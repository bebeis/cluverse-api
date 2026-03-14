-- ============================================================
-- Post seed
-- Target: 2,000,000 posts
-- Dependencies:
--   - Run after 02_member_seed.sql
-- Includes:
--   - board (120 rows)
--   - post
--   - post_tag (sparse)
--   - post_image (sparse)
-- ============================================================

SET @BOARD_START_ID = 2000001;
SET @BOARD_COUNT = 120;
SET @BOARD_END_ID = @BOARD_START_ID + @BOARD_COUNT - 1;
SET @POST_START_ID = 3000001;
SET @POST_COUNT = 2000000;
SET @POST_END_ID = @POST_START_ID + @POST_COUNT - 1;

DELETE FROM post_tag
WHERE post_id BETWEEN @POST_START_ID AND @POST_END_ID;

DELETE FROM post_image
WHERE post_id BETWEEN @POST_START_ID AND @POST_END_ID;

DELETE FROM post
WHERE post_id BETWEEN @POST_START_ID AND @POST_END_ID;

DELETE FROM board
WHERE board_id BETWEEN @BOARD_START_ID AND @BOARD_END_ID;

DROP TEMPORARY TABLE IF EXISTS tmp_seed_digits;
CREATE TEMPORARY TABLE tmp_seed_digits (
    digit TINYINT UNSIGNED NOT NULL PRIMARY KEY
) ENGINE=MEMORY;

INSERT INTO tmp_seed_digits (digit) VALUES
    (0), (1), (2), (3), (4), (5), (6), (7), (8), (9);

DROP TEMPORARY TABLE IF EXISTS tmp_seed_seq_10000;
CREATE TEMPORARY TABLE tmp_seed_seq_10000
ENGINE=MEMORY
AS
SELECT
    ones.digit
    + tens.digit * 10
    + hundreds.digit * 100
    + thousands.digit * 1000 AS seq
FROM tmp_seed_digits ones
CROSS JOIN tmp_seed_digits tens
CROSS JOIN tmp_seed_digits hundreds
CROSS JOIN tmp_seed_digits thousands;

ALTER TABLE tmp_seed_seq_10000 ADD PRIMARY KEY (seq);

INSERT INTO board (
    board_id,
    board_type,
    name,
    description,
    parent_id,
    depth,
    display_order,
    is_active,
    created_at,
    updated_at
)
SELECT
    @BOARD_START_ID + seq AS board_id,
    CASE
        WHEN seq < 60 THEN 'DEPARTMENT'
        ELSE 'INTEREST'
    END AS board_type,
    CONCAT('Seed Board ', LPAD(seq + 1, 3, '0')) AS name,
    CONCAT('Generated board ', LPAD(seq + 1, 3, '0')) AS description,
    NULL AS parent_id,
    0 AS depth,
    seq + 1 AS display_order,
    TRUE AS is_active,
    NOW() AS created_at,
    NOW() AS updated_at
FROM tmp_seed_seq_10000
WHERE seq < @BOARD_COUNT;

DELIMITER $$

DROP PROCEDURE IF EXISTS seed_posts $$
CREATE PROCEDURE seed_posts()
BEGIN
    DECLARE v_offset INT DEFAULT 0;
    DECLARE v_batch_size INT DEFAULT 10000;
    DECLARE v_total INT DEFAULT 2000000;
    DECLARE v_start BIGINT DEFAULT 3000001;

    WHILE v_offset < v_total DO
        INSERT INTO post (
            post_id,
            board_id,
            member_id,
            title,
            content,
            category,
            is_anonymous,
            is_pinned,
            is_external_visible,
            status,
            view_count,
            like_count,
            comment_count,
            bookmark_count,
            deleted_at,
            client_ip,
            created_at,
            updated_at
        )
        SELECT
            v_start + v_offset + seq AS post_id,
            2000001 + MOD(v_offset + seq, 120) AS board_id,
            1000001 + MOD(v_offset + seq, 50000) AS member_id,
            CONCAT('Seed post ', LPAD(v_offset + seq + 1, 7, '0')) AS title,
            CONCAT(
                'Generated content for seed post ',
                LPAD(v_offset + seq + 1, 7, '0'),
                '. This row is intended for large-volume local testing.'
            ) AS content,
            CASE MOD(v_offset + seq, 7)
                WHEN 0 THEN 'NOTICE'
                WHEN 1 THEN 'GENERAL'
                WHEN 2 THEN 'QUESTION'
                WHEN 3 THEN 'INFORMATION'
                WHEN 4 THEN 'REVIEW'
                WHEN 5 THEN 'RESOURCE'
                ELSE 'RECRUITMENT'
            END AS category,
            CASE
                WHEN MOD(v_offset + seq + 1, 11) = 0 THEN TRUE
                ELSE FALSE
            END AS is_anonymous,
            CASE
                WHEN MOD(v_offset + seq + 1, 5000) = 0 THEN TRUE
                ELSE FALSE
            END AS is_pinned,
            CASE
                WHEN MOD(v_offset + seq + 1, 13) = 0 THEN FALSE
                ELSE TRUE
            END AS is_external_visible,
            CASE
                WHEN MOD(v_offset + seq + 1, 1000) = 0 THEN 'DELETED'
                WHEN MOD(v_offset + seq + 1, 300) = 0 THEN 'BLINDED'
                ELSE 'ACTIVE'
            END AS status,
            MOD((v_offset + seq + 1) * 17, 20000) AS view_count,
            MOD((v_offset + seq + 1) * 7, 300) AS like_count,
            0 AS comment_count,
            MOD((v_offset + seq + 1) * 3, 120) AS bookmark_count,
            CASE
                WHEN MOD(v_offset + seq + 1, 1000) = 0 THEN DATE_SUB(NOW(), INTERVAL MOD(v_offset + seq, 90) DAY)
                ELSE NULL
            END AS deleted_at,
            CONCAT(
                '172.',
                MOD(v_offset + seq, 250),
                '.',
                MOD(FLOOR((v_offset + seq) / 250), 250),
                '.',
                MOD(v_offset + seq, 250)
            ) AS client_ip,
            DATE_SUB(NOW(), INTERVAL MOD(v_offset + seq, 1440) HOUR) AS created_at,
            NOW() AS updated_at
        FROM tmp_seed_seq_10000
        WHERE seq < LEAST(v_batch_size, v_total - v_offset);

        SET v_offset = v_offset + v_batch_size;
    END WHILE;
END $$

CALL seed_posts() $$
DROP PROCEDURE seed_posts $$

DELIMITER ;

INSERT INTO post_tag (
    post_id,
    tag_name
)
SELECT
    post_id,
    CONCAT('seed-tag-', LPAD(MOD(post_id - @POST_START_ID, 20) + 1, 2, '0')) AS tag_name
FROM post
WHERE post_id BETWEEN @POST_START_ID AND @POST_END_ID
  AND MOD(post_id, 5) = 0;

INSERT INTO post_image (
    post_id,
    image_url,
    display_order
)
SELECT
    post_id,
    CONCAT('https://cdn.seed.local/post/', post_id, '/image-01.png') AS image_url,
    0 AS display_order
FROM post
WHERE post_id BETWEEN @POST_START_ID AND @POST_END_ID
  AND MOD(post_id, 20) = 0;

ALTER TABLE board AUTO_INCREMENT = 2000121;
ALTER TABLE post AUTO_INCREMENT = 5000001;

DROP TEMPORARY TABLE IF EXISTS tmp_seed_seq_10000;
DROP TEMPORARY TABLE IF EXISTS tmp_seed_digits;
