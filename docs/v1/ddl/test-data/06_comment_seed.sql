-- ============================================================
-- Comment seed
-- Target: 3,000,000 comments
-- Dependencies:
--   - Run after 05_post_seed.sql
-- Includes:
--   - comment
--   - updates post.comment_count
--   - updates comment.reply_count
-- ============================================================

SET @COMMENT_START_ID = 6000001;
SET @COMMENT_COUNT = 3000000;
SET @COMMENT_END_ID = @COMMENT_START_ID + @COMMENT_COUNT - 1;
SET @POST_START_ID = 3000001;
SET @POST_END_ID = 5000000;

UPDATE post
SET comment_count = 0
WHERE post_id BETWEEN @POST_START_ID AND @POST_END_ID;

DELETE FROM comment
WHERE comment_id BETWEEN @COMMENT_START_ID AND @COMMENT_END_ID;

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

UPDATE post seeded_post
JOIN (
    SELECT
        post_id,
        COUNT(*) AS comment_count
    FROM comment
    WHERE comment_id BETWEEN @COMMENT_START_ID AND @COMMENT_END_ID
    GROUP BY post_id
) comment_count_summary
    ON comment_count_summary.post_id = seeded_post.post_id
SET seeded_post.comment_count = comment_count_summary.comment_count
WHERE seeded_post.post_id BETWEEN @POST_START_ID AND @POST_END_ID;

ALTER TABLE comment AUTO_INCREMENT = 9000001;

DROP TEMPORARY TABLE IF EXISTS tmp_seed_seq_10000;
DROP TEMPORARY TABLE IF EXISTS tmp_seed_digits;
