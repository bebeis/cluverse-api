-- ============================================================
-- Additional post seed (8,000,000 rows)
-- Target:
--   - post: 6,000,000 rows → board 2001001 (existing popular board)
--   - post: 2,000,000 rows → board 2001002 (new secondary board)
-- Dependencies:
--   - Run after 05a_popular_board_post_seed.sql
-- Ranges:
--   - board_id : 2001001 (reuse), 2001002 (new)
--   - post_id  : 6000001 ~ 13999999
-- ============================================================

SET @POPULAR_BOARD_ID    = 2001001;
SET @SECONDARY_BOARD_ID  = 2001002;

SET @POPULAR_POST_START  = 6000001;
SET @POPULAR_POST_COUNT  = 6000000;
SET @POPULAR_POST_END    = @POPULAR_POST_START + @POPULAR_POST_COUNT - 1;

SET @SECONDARY_POST_START = 12000001;
SET @SECONDARY_POST_COUNT = 2000000;
SET @SECONDARY_POST_END   = @SECONDARY_POST_START + @SECONDARY_POST_COUNT - 1;

-- ------------------------------------------------------------
-- cleanup
-- ------------------------------------------------------------
DELETE FROM post_tag
WHERE post_id BETWEEN @POPULAR_POST_START AND @POPULAR_POST_END
   OR post_id BETWEEN @SECONDARY_POST_START AND @SECONDARY_POST_END;

DELETE FROM post_image
WHERE post_id BETWEEN @POPULAR_POST_START AND @POPULAR_POST_END
   OR post_id BETWEEN @SECONDARY_POST_START AND @SECONDARY_POST_END;

DELETE FROM post_view_count
WHERE post_id BETWEEN @POPULAR_POST_START AND @POPULAR_POST_END
   OR post_id BETWEEN @SECONDARY_POST_START AND @SECONDARY_POST_END;

DELETE FROM post_like_count
WHERE post_id BETWEEN @POPULAR_POST_START AND @POPULAR_POST_END
   OR post_id BETWEEN @SECONDARY_POST_START AND @SECONDARY_POST_END;

DELETE FROM post_comment_count
WHERE post_id BETWEEN @POPULAR_POST_START AND @POPULAR_POST_END
   OR post_id BETWEEN @SECONDARY_POST_START AND @SECONDARY_POST_END;

DELETE FROM post_bookmark_count
WHERE post_id BETWEEN @POPULAR_POST_START AND @POPULAR_POST_END
   OR post_id BETWEEN @SECONDARY_POST_START AND @SECONDARY_POST_END;

DELETE FROM post
WHERE post_id BETWEEN @POPULAR_POST_START AND @POPULAR_POST_END
   OR post_id BETWEEN @SECONDARY_POST_START AND @SECONDARY_POST_END;

DELETE FROM board
WHERE board_id = @SECONDARY_BOARD_ID;

-- ------------------------------------------------------------
-- secondary board
-- ------------------------------------------------------------
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
    @SECONDARY_BOARD_ID,
    'INTEREST',
    'Seed Secondary Board',
    'Synthetic secondary board for multi-board query testing',
    NULL,
    0,
    1002,
    TRUE,
    NOW(),
    NOW()
);

-- ------------------------------------------------------------
-- seq helper table (0 ~ 9999)
-- ------------------------------------------------------------
DROP TEMPORARY TABLE IF EXISTS tmp_seed_seq_10000;
CREATE TEMPORARY TABLE tmp_seed_seq_10000
ENGINE=MEMORY
AS
SELECT
    ones.digit
    + tens.digit      * 10
    + hundreds.digit  * 100
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

DELIMITER $$

-- ------------------------------------------------------------
-- popular board: 6,000,000 posts (post_id 6000001 ~ 11999999)
-- ------------------------------------------------------------
DROP PROCEDURE IF EXISTS seed_popular_posts_6m $$
CREATE PROCEDURE seed_popular_posts_6m()
BEGIN
    DECLARE v_offset     INT    DEFAULT 0;
    DECLARE v_batch_size INT    DEFAULT 10000;
    DECLARE v_total      INT    DEFAULT 6000000;
    DECLARE v_start      BIGINT DEFAULT 6000001;

    WHILE v_offset < v_total DO
        INSERT INTO post (
            post_id, board_id, member_id, title, content, category,
            is_anonymous, is_pinned, is_external_visible, status,
            deleted_at, client_ip, created_at, updated_at
        )
        SELECT
            v_start + v_offset + seq,
            @POPULAR_BOARD_ID,
            1000001 + MOD((v_offset + seq) * 7, 50000),
            CONCAT('Popular board post ', LPAD(v_offset + seq + 1000001, 8, '0')),
            CONCAT(
                'Generated content for popular board post ',
                LPAD(v_offset + seq + 1000001, 8, '0'),
                '. This dataset concentrates a large number of rows into a single board for hot-board query testing.'
            ),
            CASE MOD(v_offset + seq, 6)
                WHEN 0 THEN 'GENERAL'
                WHEN 1 THEN 'QUESTION'
                WHEN 2 THEN 'INFORMATION'
                WHEN 3 THEN 'REVIEW'
                WHEN 4 THEN 'RESOURCE'
                ELSE 'RECRUITMENT'
            END,
            MOD(v_offset + seq + 1, 9) = 0,
            MOD(v_offset + seq + 1, 50000) = 0,
            TRUE,
            CASE
                WHEN MOD(v_offset + seq + 1, 5000) = 0 THEN 'DELETED'
                WHEN MOD(v_offset + seq + 1, 1200) = 0 THEN 'BLINDED'
                ELSE 'ACTIVE'
            END,
            CASE
                WHEN MOD(v_offset + seq + 1, 5000) = 0
                THEN DATE_SUB(NOW(), INTERVAL MOD(v_offset + seq, 30) DAY)
                ELSE NULL
            END,
            CONCAT(
                '192.', MOD(v_offset + seq, 250), '.',
                MOD(FLOOR((v_offset + seq) / 250), 250), '.',
                MOD((v_offset + seq) * 3, 250)
            ),
            DATE_SUB(NOW(), INTERVAL MOD(v_offset + seq, 336) HOUR),
            NOW()
        FROM tmp_seed_seq_10000
        WHERE seq < LEAST(v_batch_size, v_total - v_offset);

        SET v_offset = v_offset + v_batch_size;
    END WHILE;
END $$

CALL seed_popular_posts_6m() $$
DROP PROCEDURE seed_popular_posts_6m $$

-- ------------------------------------------------------------
-- secondary board: 2,000,000 posts (post_id 12000001 ~ 13999999)
-- ------------------------------------------------------------
DROP PROCEDURE IF EXISTS seed_secondary_posts_2m $$
CREATE PROCEDURE seed_secondary_posts_2m()
BEGIN
    DECLARE v_offset     INT    DEFAULT 0;
    DECLARE v_batch_size INT    DEFAULT 10000;
    DECLARE v_total      INT    DEFAULT 2000000;
    DECLARE v_start      BIGINT DEFAULT 12000001;

    WHILE v_offset < v_total DO
        INSERT INTO post (
            post_id, board_id, member_id, title, content, category,
            is_anonymous, is_pinned, is_external_visible, status,
            deleted_at, client_ip, created_at, updated_at
        )
        SELECT
            v_start + v_offset + seq,
            @SECONDARY_BOARD_ID,
            1000001 + MOD((v_offset + seq) * 13, 50000),
            CONCAT('Secondary board post ', LPAD(v_offset + seq + 1, 7, '0')),
            CONCAT(
                'Generated content for secondary board post ',
                LPAD(v_offset + seq + 1, 7, '0'),
                '. This dataset is used for multi-board comparison query testing.'
            ),
            CASE MOD(v_offset + seq, 6)
                WHEN 0 THEN 'GENERAL'
                WHEN 1 THEN 'QUESTION'
                WHEN 2 THEN 'INFORMATION'
                WHEN 3 THEN 'REVIEW'
                WHEN 4 THEN 'RESOURCE'
                ELSE 'RECRUITMENT'
            END,
            MOD(v_offset + seq + 1, 9) = 0,
            MOD(v_offset + seq + 1, 50000) = 0,
            TRUE,
            CASE
                WHEN MOD(v_offset + seq + 1, 5000) = 0 THEN 'DELETED'
                WHEN MOD(v_offset + seq + 1, 1200) = 0 THEN 'BLINDED'
                ELSE 'ACTIVE'
            END,
            CASE
                WHEN MOD(v_offset + seq + 1, 5000) = 0
                THEN DATE_SUB(NOW(), INTERVAL MOD(v_offset + seq, 30) DAY)
                ELSE NULL
            END,
            CONCAT(
                '10.', MOD(v_offset + seq, 250), '.',
                MOD(FLOOR((v_offset + seq) / 250), 250), '.',
                MOD((v_offset + seq) * 3, 250)
            ),
            DATE_SUB(NOW(), INTERVAL MOD(v_offset + seq, 720) HOUR),
            NOW()
        FROM tmp_seed_seq_10000
        WHERE seq < LEAST(v_batch_size, v_total - v_offset);

        SET v_offset = v_offset + v_batch_size;
    END WHILE;
END $$

CALL seed_secondary_posts_2m() $$
DROP PROCEDURE seed_secondary_posts_2m $$

DELIMITER ;

INSERT INTO post_view_count (
    post_id,
    view_count,
    created_at,
    updated_at
)
SELECT
    post_id,
    5000 + MOD((post_id - @POPULAR_POST_START + 1) * 29, 150000) AS view_count,
    created_at,
    updated_at
FROM post
WHERE post_id BETWEEN @POPULAR_POST_START AND @POPULAR_POST_END;

INSERT INTO post_view_count (
    post_id,
    view_count,
    created_at,
    updated_at
)
SELECT
    post_id,
    1000 + MOD((post_id - @SECONDARY_POST_START + 1) * 17, 50000) AS view_count,
    created_at,
    updated_at
FROM post
WHERE post_id BETWEEN @SECONDARY_POST_START AND @SECONDARY_POST_END;

INSERT INTO post_like_count (
    post_id,
    like_count,
    created_at,
    updated_at
)
SELECT
    post_id,
    100 + MOD((post_id - @POPULAR_POST_START + 1) * 11, 5000) AS like_count,
    created_at,
    updated_at
FROM post
WHERE post_id BETWEEN @POPULAR_POST_START AND @POPULAR_POST_END;

INSERT INTO post_like_count (
    post_id,
    like_count,
    created_at,
    updated_at
)
SELECT
    post_id,
    10 + MOD((post_id - @SECONDARY_POST_START + 1) * 7, 2000) AS like_count,
    created_at,
    updated_at
FROM post
WHERE post_id BETWEEN @SECONDARY_POST_START AND @SECONDARY_POST_END;

INSERT INTO post_bookmark_count (
    post_id,
    bookmark_count,
    created_at,
    updated_at
)
SELECT
    post_id,
    30 + MOD((post_id - @POPULAR_POST_START + 1) * 5, 1500) AS bookmark_count,
    created_at,
    updated_at
FROM post
WHERE post_id BETWEEN @POPULAR_POST_START AND @POPULAR_POST_END;

INSERT INTO post_bookmark_count (
    post_id,
    bookmark_count,
    created_at,
    updated_at
)
SELECT
    post_id,
    5 + MOD((post_id - @SECONDARY_POST_START + 1) * 3, 500) AS bookmark_count,
    created_at,
    updated_at
FROM post
WHERE post_id BETWEEN @SECONDARY_POST_START AND @SECONDARY_POST_END;

-- ------------------------------------------------------------
-- post_tag (sparse: 1/4)
-- ------------------------------------------------------------
INSERT INTO post_tag (post_id, tag_name)
SELECT
    post_id,
    CONCAT('popular-tag-', LPAD(MOD(post_id - @POPULAR_POST_START, 12) + 1, 2, '0'))
FROM post
WHERE post_id BETWEEN @POPULAR_POST_START AND @POPULAR_POST_END
  AND MOD(post_id, 4) = 0;

INSERT INTO post_tag (post_id, tag_name)
SELECT
    post_id,
    CONCAT('secondary-tag-', LPAD(MOD(post_id - @SECONDARY_POST_START, 10) + 1, 2, '0'))
FROM post
WHERE post_id BETWEEN @SECONDARY_POST_START AND @SECONDARY_POST_END
  AND MOD(post_id, 4) = 0;

-- ------------------------------------------------------------
-- post_image (sparse: 1/10)
-- ------------------------------------------------------------
INSERT INTO post_image (post_id, image_url, display_order)
SELECT
    post_id,
    CONCAT('https://cdn.seed.local/popular-board/', post_id, '/image-01.png'),
    0
FROM post
WHERE post_id BETWEEN @POPULAR_POST_START AND @POPULAR_POST_END
  AND MOD(post_id, 10) = 0;

INSERT INTO post_image (post_id, image_url, display_order)
SELECT
    post_id,
    CONCAT('https://cdn.seed.local/secondary-board/', post_id, '/image-01.png'),
    0
FROM post
WHERE post_id BETWEEN @SECONDARY_POST_START AND @SECONDARY_POST_END
  AND MOD(post_id, 10) = 0;

-- ------------------------------------------------------------
-- AUTO_INCREMENT reset
-- ------------------------------------------------------------
ALTER TABLE board AUTO_INCREMENT = 2001003;
ALTER TABLE post  AUTO_INCREMENT = 14000001;

DROP TEMPORARY TABLE IF EXISTS tmp_seed_seq_10000;
