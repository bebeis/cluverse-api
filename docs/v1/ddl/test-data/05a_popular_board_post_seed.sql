-- ============================================================
-- Popular board post seed
-- Target:
--   - board: 1 row
--   - post: 1,000,000 rows
-- Dependencies:
--   - Run after 02_member_seed.sql
-- Recommended order:
--   - Run after 05_post_seed.sql when you want an additional hot board
-- Includes:
--   - board (popular board only)
--   - post
--   - post_tag (sparse)
--   - post_image (sparse)
-- ============================================================

SET @POPULAR_BOARD_ID = 2001001;
SET @POPULAR_POST_START_ID = 5000001;
SET @POPULAR_POST_COUNT = 1000000;
SET @POPULAR_POST_END_ID = @POPULAR_POST_START_ID + @POPULAR_POST_COUNT - 1;

DELETE FROM post_tag
WHERE post_id BETWEEN @POPULAR_POST_START_ID AND @POPULAR_POST_END_ID;

DELETE FROM post_image
WHERE post_id BETWEEN @POPULAR_POST_START_ID AND @POPULAR_POST_END_ID;

DELETE FROM post
WHERE post_id BETWEEN @POPULAR_POST_START_ID AND @POPULAR_POST_END_ID;

DELETE FROM board
WHERE board_id = @POPULAR_BOARD_ID;

DROP TEMPORARY TABLE IF EXISTS tmp_seed_seq_10000;
CREATE TEMPORARY TABLE tmp_seed_seq_10000
ENGINE=MEMORY
AS
SELECT
    ones.digit
    + tens.digit * 10
    + hundreds.digit * 100
    + thousands.digit * 1000 AS seq
FROM (
    SELECT 0 AS digit UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
    UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
) ones
CROSS JOIN (
    SELECT 0 AS digit UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
    UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
) tens
CROSS JOIN (
    SELECT 0 AS digit UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
    UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
) hundreds
CROSS JOIN (
    SELECT 0 AS digit UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
    UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
) thousands;

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
) VALUES (
    @POPULAR_BOARD_ID,
    'INTEREST',
    'Seed Popular Board',
    'Synthetic hot board for concentrated high-volume post queries',
    NULL,
    0,
    1001,
    TRUE,
    NOW(),
    NOW()
);

DELIMITER $$

DROP PROCEDURE IF EXISTS seed_popular_board_posts $$
CREATE PROCEDURE seed_popular_board_posts()
BEGIN
    DECLARE v_offset INT DEFAULT 0;
    DECLARE v_batch_size INT DEFAULT 10000;
    DECLARE v_total INT DEFAULT 1000000;
    DECLARE v_start BIGINT DEFAULT 5000001;

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
            @POPULAR_BOARD_ID AS board_id,
            1000001 + MOD((v_offset + seq) * 7, 50000) AS member_id,
            CONCAT('Popular board post ', LPAD(v_offset + seq + 1, 7, '0')) AS title,
            CONCAT(
                'Generated content for popular board post ',
                LPAD(v_offset + seq + 1, 7, '0'),
                '. This dataset concentrates a large number of rows into a single board for hot-board query testing.'
            ) AS content,
            CASE MOD(v_offset + seq, 6)
                WHEN 0 THEN 'GENERAL'
                WHEN 1 THEN 'QUESTION'
                WHEN 2 THEN 'INFORMATION'
                WHEN 3 THEN 'REVIEW'
                WHEN 4 THEN 'RESOURCE'
                ELSE 'RECRUITMENT'
            END AS category,
            CASE
                WHEN MOD(v_offset + seq + 1, 9) = 0 THEN TRUE
                ELSE FALSE
            END AS is_anonymous,
            CASE
                WHEN MOD(v_offset + seq + 1, 50000) = 0 THEN TRUE
                ELSE FALSE
            END AS is_pinned,
            TRUE AS is_external_visible,
            CASE
                WHEN MOD(v_offset + seq + 1, 5000) = 0 THEN 'DELETED'
                WHEN MOD(v_offset + seq + 1, 1200) = 0 THEN 'BLINDED'
                ELSE 'ACTIVE'
            END AS status,
            5000 + MOD((v_offset + seq + 1) * 29, 150000) AS view_count,
            100 + MOD((v_offset + seq + 1) * 11, 5000) AS like_count,
            0 AS comment_count,
            30 + MOD((v_offset + seq + 1) * 5, 1500) AS bookmark_count,
            CASE
                WHEN MOD(v_offset + seq + 1, 5000) = 0 THEN DATE_SUB(NOW(), INTERVAL MOD(v_offset + seq, 30) DAY)
                ELSE NULL
            END AS deleted_at,
            CONCAT(
                '192.',
                MOD(v_offset + seq, 250),
                '.',
                MOD(FLOOR((v_offset + seq) / 250), 250),
                '.',
                MOD((v_offset + seq) * 3, 250)
            ) AS client_ip,
            DATE_SUB(NOW(), INTERVAL MOD(v_offset + seq, 336) HOUR) AS created_at,
            NOW() AS updated_at
        FROM tmp_seed_seq_10000
        WHERE seq < LEAST(v_batch_size, v_total - v_offset);

        SET v_offset = v_offset + v_batch_size;
    END WHILE;
END $$

CALL seed_popular_board_posts() $$
DROP PROCEDURE seed_popular_board_posts $$

DELIMITER ;

INSERT INTO post_tag (
    post_id,
    tag_name
)
SELECT
    post_id,
    CONCAT('popular-tag-', LPAD(MOD(post_id - @POPULAR_POST_START_ID, 12) + 1, 2, '0')) AS tag_name
FROM post
WHERE post_id BETWEEN @POPULAR_POST_START_ID AND @POPULAR_POST_END_ID
  AND MOD(post_id, 4) = 0;

INSERT INTO post_image (
    post_id,
    image_url,
    display_order
)
SELECT
    post_id,
    CONCAT('https://cdn.seed.local/popular-board/', post_id, '/image-01.png') AS image_url,
    0 AS display_order
FROM post
WHERE post_id BETWEEN @POPULAR_POST_START_ID AND @POPULAR_POST_END_ID
  AND MOD(post_id, 10) = 0;

ALTER TABLE board AUTO_INCREMENT = 2001002;
ALTER TABLE post AUTO_INCREMENT = 6000001;

DROP TEMPORARY TABLE IF EXISTS tmp_seed_seq_10000;
