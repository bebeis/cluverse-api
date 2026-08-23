SET @benchmark_member_id = COALESCE(@benchmark_member_id, 1);
SET @benchmark_board_id = COALESCE(@benchmark_board_id, 1);
SET @benchmark_post_count = COALESCE(@benchmark_post_count, 1000);
SET @benchmark_comment_count = COALESCE(@benchmark_comment_count, 100000);
SET @benchmark_hot_comment_percent = COALESCE(@benchmark_hot_comment_percent, 20);
SET @benchmark_post_base_time = COALESCE(@benchmark_post_base_time, '2026-08-22 00:00:00');
SET @benchmark_comment_base_time = COALESCE(@benchmark_comment_base_time, '2026-08-23 00:00:00');

DROP TEMPORARY TABLE IF EXISTS tmp_home_feed_seq_10000;
CREATE TEMPORARY TABLE tmp_home_feed_seq_10000
ENGINE=MEMORY
AS
SELECT ones.digit
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

ALTER TABLE tmp_home_feed_seq_10000 ADD PRIMARY KEY (seq);

DROP TEMPORARY TABLE IF EXISTS tmp_home_feed_posts;
CREATE TEMPORARY TABLE tmp_home_feed_posts (
    ordinal INT NOT NULL,
    post_id BIGINT NOT NULL,
    PRIMARY KEY (ordinal),
    UNIQUE KEY uk_tmp_home_feed_post (post_id)
) ENGINE=InnoDB;

DROP PROCEDURE IF EXISTS seed_home_feed_fixture;

DELIMITER //
CREATE PROCEDURE seed_home_feed_fixture(
    IN target_member_id BIGINT,
    IN target_board_id BIGINT,
    IN post_count INT,
    IN comment_count INT,
    IN hot_comment_percent INT
)
BEGIN
    DECLARE post_offset INT DEFAULT 0;
    DECLARE comment_offset INT DEFAULT 0;
    DECLARE batch_size INT DEFAULT 10000;
    DECLARE hot_comment_count INT;
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        RESIGNAL;
    END;

    IF post_count < 10 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'post_count는 10 이상이어야 합니다.';
    END IF;
    IF comment_count < post_count THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'comment_count는 post_count 이상이어야 합니다.';
    END IF;
    IF hot_comment_percent < 0 OR hot_comment_percent > 90 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'hot_comment_percent는 0 이상 90 이하여야 합니다.';
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM member WHERE member_id = target_member_id AND status = 'ACTIVE'
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '활성 benchmark_member_id가 필요합니다.';
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM board
        WHERE board_id = target_board_id AND board_type <> 'GROUP' AND is_active = TRUE
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '활성 공개 benchmark_board_id가 필요합니다.';
    END IF;

    DELETE activity
    FROM post_comment_activity activity
    JOIN post p ON p.post_id = activity.post_id
    WHERE p.client_ip = 'benchmark-home-feed';

    DELETE comment_like
    FROM comment_like
    JOIN comment c ON c.comment_id = comment_like.comment_id
    JOIN post p ON p.post_id = c.post_id
    WHERE p.client_ip = 'benchmark-home-feed';

    DELETE comment_place
    FROM comment_place
    JOIN comment c ON c.comment_id = comment_place.comment_id
    JOIN post p ON p.post_id = c.post_id
    WHERE p.client_ip = 'benchmark-home-feed';

    DELETE c
    FROM comment c
    JOIN post p ON p.post_id = c.post_id
    WHERE p.client_ip = 'benchmark-home-feed';

    DELETE post_comment_count
    FROM post_comment_count
    JOIN post p ON p.post_id = post_comment_count.post_id
    WHERE p.client_ip = 'benchmark-home-feed';

    DELETE FROM post WHERE client_ip = 'benchmark-home-feed';

    WHILE post_offset < post_count DO
        INSERT INTO post (
            board_id, member_id, title, content, category,
            is_anonymous, is_pinned, is_external_visible, status,
            client_ip, created_at, updated_at
        )
        SELECT
            target_board_id,
            target_member_id,
            CONCAT('Home feed benchmark post ', post_offset + seq + 1),
            'Controlled fixture for recent commented post measurement',
            'GENERAL',
            FALSE,
            FALSE,
            TRUE,
            'ACTIVE',
            'benchmark-home-feed',
            DATE_ADD(@benchmark_post_base_time, INTERVAL post_offset + seq SECOND),
            DATE_ADD(@benchmark_post_base_time, INTERVAL post_offset + seq SECOND)
        FROM tmp_home_feed_seq_10000
        WHERE seq < LEAST(batch_size, post_count - post_offset)
        ORDER BY seq;

        SET post_offset = post_offset + batch_size;
    END WHILE;

    INSERT INTO tmp_home_feed_posts (ordinal, post_id)
    SELECT CAST(SUBSTRING_INDEX(p.title, ' ', -1) AS UNSIGNED), p.post_id
    FROM post p
    WHERE p.client_ip = 'benchmark-home-feed';

    SET hot_comment_count = FLOOR(comment_count * hot_comment_percent / 100);
    WHILE comment_offset < comment_count DO
        INSERT INTO comment (
            post_id, member_id, parent_id, depth, content,
            is_anonymous, status, like_count, reply_count,
            client_ip, created_at, updated_at, path
        )
        SELECT
            mapped.post_id,
            target_member_id,
            NULL,
            0,
            CONCAT('Home feed benchmark comment ', comment_offset + sequence.seq + 1),
            FALSE,
            'ACTIVE',
            0,
            0,
            'benchmark-home-feed',
            DATE_ADD(@benchmark_comment_base_time, INTERVAL comment_offset + sequence.seq SECOND),
            DATE_ADD(@benchmark_comment_base_time, INTERVAL comment_offset + sequence.seq SECOND),
            NULL
        FROM tmp_home_feed_seq_10000 sequence
        JOIN tmp_home_feed_posts mapped
          ON mapped.ordinal = CASE
                WHEN comment_offset + sequence.seq < hot_comment_count THEN 1
                ELSE 2 + MOD(comment_offset + sequence.seq - hot_comment_count, post_count - 1)
             END
        WHERE sequence.seq < LEAST(batch_size, comment_count - comment_offset)
        ORDER BY sequence.seq;

        SET comment_offset = comment_offset + batch_size;
    END WHILE;

    UPDATE comment
    SET path = CONCAT(
        DATE_FORMAT(created_at, '%Y%m%d%H%i%s'), '-',
        LPAD(comment_id, 20, '0')
    )
    WHERE client_ip = 'benchmark-home-feed';

    INSERT INTO post_comment_count (post_id, comment_count, created_at, updated_at)
    SELECT c.post_id, COUNT(*), NOW(), NOW()
    FROM comment c
    JOIN post p ON p.post_id = c.post_id
    WHERE p.client_ip = 'benchmark-home-feed'
    GROUP BY c.post_id;

    INSERT INTO post_comment_activity (
        post_id, last_comment_id, last_commented_at, created_at, updated_at
    )
    SELECT ranked.post_id, ranked.comment_id, ranked.created_at, NOW(), NOW()
    FROM (
        SELECT c.post_id,
               c.comment_id,
               c.created_at,
               ROW_NUMBER() OVER (
                   PARTITION BY c.post_id
                   ORDER BY c.created_at DESC, c.comment_id DESC
               ) AS row_num
        FROM comment c
        JOIN post p ON p.post_id = c.post_id
        WHERE p.client_ip = 'benchmark-home-feed'
          AND c.status <> 'DELETED'
    ) ranked
    WHERE ranked.row_num = 1;
END //
DELIMITER ;

CALL seed_home_feed_fixture(
    @benchmark_member_id,
    @benchmark_board_id,
    @benchmark_post_count,
    @benchmark_comment_count,
    @benchmark_hot_comment_percent
);

DROP PROCEDURE seed_home_feed_fixture;
DROP TEMPORARY TABLE tmp_home_feed_posts;
DROP TEMPORARY TABLE tmp_home_feed_seq_10000;

ANALYZE TABLE comment, post_comment_activity;

SELECT
    @benchmark_post_count AS requested_posts,
    @benchmark_comment_count AS requested_comments,
    @benchmark_hot_comment_percent AS hot_comment_percent,
    COUNT(DISTINCT p.post_id) AS actual_posts,
    COUNT(c.comment_id) AS actual_comments,
    COUNT(DISTINCT activity.post_id) AS activity_rows
FROM post p
LEFT JOIN comment c ON c.post_id = p.post_id
LEFT JOIN post_comment_activity activity ON activity.post_id = p.post_id
WHERE p.client_ip = 'benchmark-home-feed';
