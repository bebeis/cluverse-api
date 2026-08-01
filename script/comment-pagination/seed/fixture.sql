SET @benchmark_post_id = COALESCE(@benchmark_post_id, 1);
SET @benchmark_member_id = COALESCE(@benchmark_member_id, 1);
SET @benchmark_comment_count = COALESCE(@benchmark_comment_count, 1000);
SET @benchmark_tree_shape = COALESCE(@benchmark_tree_shape, 'mixed');

DROP PROCEDURE IF EXISTS seed_comment_pagination_fixture;

DELIMITER //
CREATE PROCEDURE seed_comment_pagination_fixture(
    IN target_post_id BIGINT,
    IN target_member_id BIGINT,
    IN comment_count INT,
    IN tree_shape VARCHAR(20)
)
BEGIN
    DECLARE sequence_number INT DEFAULT 1;
    DECLARE comment_depth INT DEFAULT 0;
    DECLARE parent_comment_id BIGINT DEFAULT NULL;
    DECLARE current_root_id BIGINT DEFAULT NULL;
    DECLARE previous_comment_id BIGINT DEFAULT NULL;
    DECLARE generated_comment_id BIGINT;
    DECLARE parent_path VARCHAR(255);
    DECLARE generated_path VARCHAR(255);
    DECLARE generated_at DATETIME;

    IF tree_shape NOT IN ('flat', 'wide', 'mixed') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'tree_shape은 flat, wide, mixed 중 하나여야 합니다.';
    END IF;

    START TRANSACTION;

    DELETE FROM comment
    WHERE post_id = target_post_id
      AND client_ip = 'benchmark-comment-pagination';

    WHILE sequence_number <= comment_count DO
        SET generated_at = DATE_ADD('2026-01-01 00:00:00', INTERVAL sequence_number SECOND);
        SET parent_comment_id = NULL;
        SET comment_depth = 0;

        IF tree_shape = 'wide' AND MOD(sequence_number - 1, 6) <> 0 THEN
            SET parent_comment_id = current_root_id;
            SET comment_depth = 1;
        ELSEIF tree_shape = 'mixed' AND MOD(sequence_number - 1, 6) <> 0 THEN
            SET parent_comment_id = previous_comment_id;
            SET comment_depth = MOD(sequence_number - 1, 6);
        END IF;

        INSERT INTO comment (
            post_id,
            member_id,
            parent_id,
            depth,
            content,
            is_anonymous,
            status,
            like_count,
            reply_count,
            client_ip,
            created_at,
            updated_at,
            path
        ) VALUES (
            target_post_id,
            target_member_id,
            parent_comment_id,
            comment_depth,
            CONCAT('comment pagination benchmark ', sequence_number),
            FALSE,
            'ACTIVE',
            0,
            0,
            'benchmark-comment-pagination',
            generated_at,
            generated_at,
            NULL
        );

        SET generated_comment_id = LAST_INSERT_ID();
        SET generated_path = CONCAT(
            DATE_FORMAT(generated_at, '%Y%m%d%H%i%s'), '-',
            LPAD(generated_comment_id, 20, '0')
        );

        IF parent_comment_id IS NOT NULL THEN
            SELECT path INTO parent_path
            FROM comment
            WHERE comment_id = parent_comment_id;
            SET generated_path = CONCAT(parent_path, '/', generated_path);
        ELSE
            SET current_root_id = generated_comment_id;
        END IF;

        UPDATE comment
        SET path = generated_path
        WHERE comment_id = generated_comment_id;

        SET previous_comment_id = generated_comment_id;
        SET sequence_number = sequence_number + 1;
    END WHILE;

    UPDATE comment parent
    LEFT JOIN (
        SELECT parent_id, COUNT(*) AS child_count
        FROM comment
        WHERE post_id = target_post_id
          AND parent_id IS NOT NULL
        GROUP BY parent_id
    ) children ON children.parent_id = parent.comment_id
    SET parent.reply_count = COALESCE(children.child_count, 0)
    WHERE parent.post_id = target_post_id;

    INSERT INTO post_comment_count (post_id, comment_count)
    SELECT target_post_id, COUNT(*)
    FROM comment
    WHERE post_id = target_post_id
    ON DUPLICATE KEY UPDATE comment_count = VALUES(comment_count);

    COMMIT;
END //
DELIMITER ;

CALL seed_comment_pagination_fixture(
    @benchmark_post_id,
    @benchmark_member_id,
    @benchmark_comment_count,
    @benchmark_tree_shape
);

DROP PROCEDURE seed_comment_pagination_fixture;

SELECT
    @benchmark_post_id AS post_id,
    @benchmark_comment_count AS requested_count,
    @benchmark_tree_shape AS tree_shape,
    COUNT(*) AS actual_count,
    MAX(depth) AS max_depth,
    SUM(path IS NULL) AS null_path_count
FROM comment
WHERE post_id = @benchmark_post_id
  AND client_ip = 'benchmark-comment-pagination';
