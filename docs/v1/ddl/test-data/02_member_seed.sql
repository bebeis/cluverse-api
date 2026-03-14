-- ============================================================
-- Member seed
-- Target: 50,000 members
-- Dependencies:
--   - Run after 01_university_seed.sql
-- Includes:
--   - member
--   - member_auth
--   - member_profile
--   - member_status_history
--   - terms
--   - member_terms_agreement
-- ============================================================

SET @MEMBER_START_ID = 1000001;
SET @MEMBER_COUNT = 50000;
SET @MEMBER_END_ID = @MEMBER_START_ID + @MEMBER_COUNT - 1;
SET @TERMS_START_ID = 1101;
SET @TERMS_END_ID = 1103;

DELETE FROM member_terms_agreement
WHERE member_id BETWEEN @MEMBER_START_ID AND @MEMBER_END_ID;

DELETE FROM member_status_history
WHERE member_id BETWEEN @MEMBER_START_ID AND @MEMBER_END_ID;

DELETE FROM member_profile
WHERE member_id BETWEEN @MEMBER_START_ID AND @MEMBER_END_ID;

DELETE FROM member_auth
WHERE member_id BETWEEN @MEMBER_START_ID AND @MEMBER_END_ID;

DELETE FROM member
WHERE member_id BETWEEN @MEMBER_START_ID AND @MEMBER_END_ID;

DELETE FROM terms
WHERE terms_id BETWEEN @TERMS_START_ID AND @TERMS_END_ID;

INSERT INTO terms (
    terms_id,
    terms_type,
    title,
    content,
    version,
    is_required,
    is_active,
    effective_at,
    created_at,
    updated_at
) VALUES
    (1101, 'SERVICE', 'Seed Terms of Service', 'Seed terms content', '1.0.0', TRUE, TRUE, NOW(), NOW(), NOW()),
    (1102, 'PRIVACY', 'Seed Privacy Policy', 'Seed privacy content', '1.0.0', TRUE, TRUE, NOW(), NOW(), NOW()),
    (1103, 'MARKETING', 'Seed Marketing Consent', 'Seed marketing content', '1.0.0', FALSE, TRUE, NOW(), NOW(), NOW());

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

DROP PROCEDURE IF EXISTS seed_members $$
CREATE PROCEDURE seed_members()
BEGIN
    DECLARE v_offset INT DEFAULT 0;
    DECLARE v_batch_size INT DEFAULT 10000;
    DECLARE v_total INT DEFAULT 50000;
    DECLARE v_start BIGINT DEFAULT 1000001;

    WHILE v_offset < v_total DO
        INSERT INTO member (
            member_id,
            nickname,
            university_id,
            status,
            verification_status,
            verification_rejected_reason,
            role,
            last_login_at,
            source_system,
            client_ip,
            created_at,
            updated_at
        )
        SELECT
            v_start + v_offset + seq AS member_id,
            CONCAT('seed_user_', LPAD(v_offset + seq + 1, 6, '0')) AS nickname,
            1001 + MOD(v_offset + seq, 30) AS university_id,
            CASE
                WHEN MOD(v_offset + seq + 1, 2000) = 0 THEN 'DELETED'
                WHEN MOD(v_offset + seq + 1, 500) = 0 THEN 'BANNED'
                WHEN MOD(v_offset + seq + 1, 100) = 0 THEN 'SUSPENDED'
                ELSE 'ACTIVE'
            END AS status,
            CASE
                WHEN MOD(v_offset + seq + 1, 9) = 0 THEN 'REJECTED'
                WHEN MOD(v_offset + seq + 1, 7) = 0 THEN 'PENDING'
                WHEN MOD(v_offset + seq + 1, 3) = 0 THEN 'APPROVED'
                ELSE 'NONE'
            END AS verification_status,
            CASE
                WHEN MOD(v_offset + seq + 1, 9) = 0 THEN 'DOCUMENT_MISMATCH'
                ELSE NULL
            END AS verification_rejected_reason,
            CASE
                WHEN v_offset + seq < 10 THEN 'ADMIN'
                WHEN v_offset + seq < 100 THEN 'MODERATOR'
                ELSE 'MEMBER'
            END AS role,
            DATE_SUB(NOW(), INTERVAL MOD(v_offset + seq, 365) DAY) AS last_login_at,
            CASE
                WHEN MOD(v_offset + seq, 2) = 0 THEN 'WEB_USER'
                ELSE 'MOBILE_APP'
            END AS source_system,
            CONCAT(
                '10.',
                MOD(v_offset + seq, 250),
                '.',
                MOD(FLOOR((v_offset + seq) / 250), 250),
                '.',
                MOD(v_offset + seq, 250)
            ) AS client_ip,
            DATE_SUB(NOW(), INTERVAL MOD(v_offset + seq, 720) HOUR) AS created_at,
            NOW() AS updated_at
        FROM tmp_seed_seq_10000
        WHERE seq < LEAST(v_batch_size, v_total - v_offset);

        SET v_offset = v_offset + v_batch_size;
    END WHILE;
END $$

CALL seed_members() $$
DROP PROCEDURE seed_members $$

DROP PROCEDURE IF EXISTS seed_member_auth $$
CREATE PROCEDURE seed_member_auth()
BEGIN
    DECLARE v_offset INT DEFAULT 0;
    DECLARE v_batch_size INT DEFAULT 10000;
    DECLARE v_total INT DEFAULT 50000;
    DECLARE v_start BIGINT DEFAULT 1000001;

    WHILE v_offset < v_total DO
        INSERT INTO member_auth (
            member_id,
            email,
            password_hash,
            created_at,
            updated_at
        )
        SELECT
            v_start + v_offset + seq AS member_id,
            CONCAT('seed_user_', LPAD(v_offset + seq + 1, 6, '0'), '@seed.local') AS email,
            '$2a$10$8l6GdC7HYSYQwNSKGwstwedK3vmbqP1O/29FLpZg2fZEGf1/WBubu' AS password_hash,
            DATE_SUB(NOW(), INTERVAL MOD(v_offset + seq, 720) HOUR) AS created_at,
            NOW() AS updated_at
        FROM tmp_seed_seq_10000
        WHERE seq < LEAST(v_batch_size, v_total - v_offset);

        SET v_offset = v_offset + v_batch_size;
    END WHILE;
END $$

CALL seed_member_auth() $$
DROP PROCEDURE seed_member_auth $$

DROP PROCEDURE IF EXISTS seed_member_profile $$
CREATE PROCEDURE seed_member_profile()
BEGIN
    DECLARE v_offset INT DEFAULT 0;
    DECLARE v_batch_size INT DEFAULT 10000;
    DECLARE v_total INT DEFAULT 50000;
    DECLARE v_start BIGINT DEFAULT 1000001;

    WHILE v_offset < v_total DO
        INSERT INTO member_profile (
            member_id,
            bio,
            profile_image_url,
            link_github,
            link_notion,
            link_portfolio,
            link_instagram,
            link_etc,
            is_public,
            visible_fields,
            created_at,
            updated_at
        )
        SELECT
            v_start + v_offset + seq AS member_id,
            CONCAT('Generated bio for seed user ', LPAD(v_offset + seq + 1, 6, '0')) AS bio,
            CONCAT('https://cdn.seed.local/profile/', LPAD(MOD(v_offset + seq + 1, 1000), 4, '0'), '.png') AS profile_image_url,
            CONCAT('https://github.com/seed-user-', LPAD(v_offset + seq + 1, 6, '0')) AS link_github,
            CONCAT('https://notion.site/seed-user-', LPAD(v_offset + seq + 1, 6, '0')) AS link_notion,
            CONCAT('https://portfolio.seed.local/users/', LPAD(v_offset + seq + 1, 6, '0')) AS link_portfolio,
            CONCAT('https://instagram.com/seed_user_', LPAD(v_offset + seq + 1, 6, '0')) AS link_instagram,
            CONCAT('https://blog.seed.local/users/', LPAD(v_offset + seq + 1, 6, '0')) AS link_etc,
            CASE
                WHEN MOD(v_offset + seq + 1, 5) = 0 THEN FALSE
                ELSE TRUE
            END AS is_public,
            CASE
                WHEN MOD(v_offset + seq + 1, 5) = 0 THEN JSON_ARRAY('BIO', 'UNIVERSITY')
                ELSE NULL
            END AS visible_fields,
            DATE_SUB(NOW(), INTERVAL MOD(v_offset + seq, 720) HOUR) AS created_at,
            NOW() AS updated_at
        FROM tmp_seed_seq_10000
        WHERE seq < LEAST(v_batch_size, v_total - v_offset);

        SET v_offset = v_offset + v_batch_size;
    END WHILE;
END $$

CALL seed_member_profile() $$
DROP PROCEDURE seed_member_profile $$

DROP PROCEDURE IF EXISTS seed_member_status_history $$
CREATE PROCEDURE seed_member_status_history()
BEGIN
    DECLARE v_offset INT DEFAULT 0;
    DECLARE v_batch_size INT DEFAULT 10000;
    DECLARE v_total INT DEFAULT 50000;
    DECLARE v_start BIGINT DEFAULT 1000001;

    WHILE v_offset < v_total DO
        INSERT INTO member_status_history (
            member_id,
            previous_status,
            new_status,
            change_type,
            change_reason,
            changed_by,
            source_system,
            client_ip,
            created_at,
            updated_at
        )
        SELECT
            v_start + v_offset + seq AS member_id,
            'NONE' AS previous_status,
            CASE
                WHEN MOD(v_offset + seq + 1, 2000) = 0 THEN 'DELETED'
                WHEN MOD(v_offset + seq + 1, 500) = 0 THEN 'BANNED'
                WHEN MOD(v_offset + seq + 1, 100) = 0 THEN 'SUSPENDED'
                ELSE 'ACTIVE'
            END AS new_status,
            'SIGNUP' AS change_type,
            NULL AS change_reason,
            NULL AS changed_by,
            'BATCH' AS source_system,
            CONCAT(
                '10.',
                MOD(v_offset + seq, 250),
                '.',
                MOD(FLOOR((v_offset + seq) / 250), 250),
                '.',
                MOD(v_offset + seq, 250)
            ) AS client_ip,
            DATE_SUB(NOW(), INTERVAL MOD(v_offset + seq, 720) HOUR) AS created_at,
            NOW() AS updated_at
        FROM tmp_seed_seq_10000
        WHERE seq < LEAST(v_batch_size, v_total - v_offset);

        SET v_offset = v_offset + v_batch_size;
    END WHILE;
END $$

CALL seed_member_status_history() $$
DROP PROCEDURE seed_member_status_history $$

DELIMITER ;

INSERT INTO member_terms_agreement (
    member_id,
    terms_id,
    agreed_at,
    created_at,
    updated_at
)
SELECT
    member_id,
    1101 AS terms_id,
    created_at,
    created_at,
    updated_at
FROM member
WHERE member_id BETWEEN @MEMBER_START_ID AND @MEMBER_END_ID;

INSERT INTO member_terms_agreement (
    member_id,
    terms_id,
    agreed_at,
    created_at,
    updated_at
)
SELECT
    member_id,
    1102 AS terms_id,
    created_at,
    created_at,
    updated_at
FROM member
WHERE member_id BETWEEN @MEMBER_START_ID AND @MEMBER_END_ID;

INSERT INTO member_terms_agreement (
    member_id,
    terms_id,
    agreed_at,
    created_at,
    updated_at
)
SELECT
    member_id,
    1103 AS terms_id,
    created_at,
    created_at,
    updated_at
FROM member
WHERE member_id BETWEEN @MEMBER_START_ID AND @MEMBER_END_ID
  AND MOD(member_id, 3) = 0;

ALTER TABLE member AUTO_INCREMENT = 1050001;
ALTER TABLE member_status_history AUTO_INCREMENT = 50001;
ALTER TABLE terms AUTO_INCREMENT = 1104;

DROP TEMPORARY TABLE IF EXISTS tmp_seed_seq_10000;
