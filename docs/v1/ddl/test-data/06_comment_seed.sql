-- ============================================================
-- Comment seed
-- Target: 3,000,000 comments
-- Dependencies:
--   - Run after 05_post_seed.sql
-- Includes:
--   - comment
--   - updates post_comment_count
--   - updates comment.reply_count
-- ============================================================

SET @COMMENT_START_ID = 6000001;
SET @COMMENT_COUNT = 3000000;
SET @COMMENT_END_ID = @COMMENT_START_ID + @COMMENT_COUNT - 1;
SET @POST_START_ID = 3000001;
SET @POST_END_ID = 5000000;

DELETE FROM post_comment_count
WHERE post_id BETWEEN @POST_START_ID AND @POST_END_ID;

DELETE FROM comment
WHERE comment_id BETWEEN @COMMENT_START_ID AND @COMMENT_END_ID;

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

DELIMITER $$

DROP PROCEDURE IF EXISTS seed_comments $$
CREATE PROCEDURE seed_comments()
BEGIN
    DECLARE v_offset INT DEFAULT 0;
    DECLARE v_batch_size INT DEFAULT 10000;
    DECLARE v_total INT DEFAULT 3000000;
    DECLARE v_start BIGINT DEFAULT 6000001;

    WHILE v_offset < v_total DO
        INSERT INTO comment (
            comment_id,
            post_id,
            member_id,
            parent_id,
            depth,
            content,
            is_anonymous,
            status,
            like_count,
            reply_count,
            deleted_at,
            client_ip,
            created_at,
            updated_at
        )
        SELECT
            v_start + v_offset + seq AS comment_id,
            CASE
                WHEN MOD(v_offset + seq + 1, 6) = 0 THEN 3000001 + MOD(v_offset + seq - 1, 2000000)
                ELSE 3000001 + MOD(v_offset + seq, 2000000)
            END AS post_id,
            1000001 + MOD((v_offset + seq) * 3, 50000) AS member_id,
            CASE
                WHEN MOD(v_offset + seq + 1, 6) = 0 THEN v_start + v_offset + seq - 1
                ELSE NULL
            END AS parent_id,
            CASE
                WHEN MOD(v_offset + seq + 1, 6) = 0 THEN 1
                ELSE 0
            END AS depth,
            CONCAT('Generated comment ', LPAD(v_offset + seq + 1, 7, '0')) AS content,
            CASE
                WHEN MOD(v_offset + seq + 1, 9) = 0 THEN TRUE
                ELSE FALSE
            END AS is_anonymous,
            CASE
                WHEN MOD(v_offset + seq + 1, 1500) = 0 THEN 'DELETED'
                WHEN MOD(v_offset + seq + 1, 400) = 0 THEN 'BLINDED'
                ELSE 'ACTIVE'
            END AS status,
            MOD((v_offset + seq + 1) * 5, 80) AS like_count,
            0 AS reply_count,
            CASE
                WHEN MOD(v_offset + seq + 1, 1500) = 0 THEN DATE_SUB(NOW(), INTERVAL MOD(v_offset + seq, 30) DAY)
                ELSE NULL
            END AS deleted_at,
            CONCAT(
                '192.',
                MOD(v_offset + seq, 250),
                '.',
                MOD(FLOOR((v_offset + seq) / 250), 250),
                '.',
                MOD(v_offset + seq, 250)
            ) AS client_ip,
            DATE_SUB(NOW(), INTERVAL MOD(v_offset + seq, 2160) HOUR) AS created_at,
            NOW() AS updated_at
        FROM tmp_seed_seq_10000
        WHERE seq < LEAST(v_batch_size, v_total - v_offset);

        SET v_offset = v_offset + v_batch_size;
    END WHILE;
END $$

CALL seed_comments() $$
DROP PROCEDURE seed_comments $$

DELIMITER ;

UPDATE comment
SET reply_count = 0
WHERE comment_id BETWEEN @COMMENT_START_ID AND @COMMENT_END_ID;

UPDATE comment parent_comment
JOIN (
    SELECT
        parent_id,
        COUNT(*) AS reply_count
    FROM comment
    WHERE comment_id BETWEEN @COMMENT_START_ID AND @COMMENT_END_ID
      AND parent_id IS NOT NULL
    GROUP BY parent_id
) child_comment_count
    ON child_comment_count.parent_id = parent_comment.comment_id
SET parent_comment.reply_count = child_comment_count.reply_count
WHERE parent_comment.comment_id BETWEEN @COMMENT_START_ID AND @COMMENT_END_ID;

INSERT INTO post_comment_count (
    post_id,
    comment_count,
    created_at,
    updated_at
)
SELECT
    post_id,
    COUNT(*) AS comment_count,
    NOW() AS created_at,
    NOW() AS updated_at
FROM comment
WHERE post_id BETWEEN @POST_START_ID AND @POST_END_ID
GROUP BY post_id;

ALTER TABLE comment AUTO_INCREMENT = 9000001;

DROP TEMPORARY TABLE IF EXISTS tmp_seed_seq_10000;
