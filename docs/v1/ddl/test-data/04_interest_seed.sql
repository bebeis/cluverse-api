-- ============================================================
-- Interest seed
-- Target:
--   - interests: 200 rows
--   - interest_major_relation: 600 rows
--   - member_interests: 100,000 rows
-- Dependencies:
--   - Run after 03_major_seed.sql
-- Includes:
--   - board (interest range only)
--   - interests
--   - interest_major_relation
--   - member_interests
-- ============================================================

SET @INTEREST_BOARD_START_ID = 2200001;
SET @INTEREST_BOARD_COUNT = 200;
SET @INTEREST_BOARD_END_ID = @INTEREST_BOARD_START_ID + @INTEREST_BOARD_COUNT - 1;
SET @INTEREST_START_ID = 3200001;
SET @INTEREST_COUNT = 200;
SET @INTEREST_END_ID = @INTEREST_START_ID + @INTEREST_COUNT - 1;
SET @MAJOR_START_ID = 3100001;
SET @MAJOR_COUNT = 300;
SET @MEMBER_START_ID = 1000001;
SET @MEMBER_END_ID = 1050000;

DELETE FROM member_interests
WHERE member_id BETWEEN @MEMBER_START_ID AND @MEMBER_END_ID
  AND interest_id BETWEEN @INTEREST_START_ID AND @INTEREST_END_ID;

DELETE FROM interest_major_relation
WHERE interest_id BETWEEN @INTEREST_START_ID AND @INTEREST_END_ID;

DELETE FROM interests
WHERE interest_id BETWEEN @INTEREST_START_ID AND @INTEREST_END_ID;

DELETE FROM board
WHERE board_id BETWEEN @INTEREST_BOARD_START_ID AND @INTEREST_BOARD_END_ID;

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
)
SELECT
    @INTEREST_BOARD_START_ID + seq AS board_id,
    'INTEREST' AS board_type,
    CASE
        WHEN seq < 40 THEN CONCAT('Interest Group Board ', LPAD(seq + 1, 3, '0'))
        ELSE CONCAT(
            'Interest Board ',
            LPAD(FLOOR((seq - 40) / 4) + 1, 3, '0'),
            '-',
            MOD(seq - 40, 4) + 1
        )
    END AS name,
    'Generated seed board for interest domain' AS description,
    NULL AS parent_id,
    CASE
        WHEN seq < 40 THEN 0
        ELSE 1
    END AS depth,
    seq + 1 AS display_order,
    TRUE AS is_active,
    NOW() AS created_at,
    NOW() AS updated_at
FROM tmp_seed_seq_10000
WHERE seq < @INTEREST_COUNT;

INSERT INTO interests (
    interest_id,
    board_id,
    name,
    category,
    parent_id,
    display_order,
    is_active,
    created_at,
    updated_at
)
SELECT
    @INTEREST_START_ID + seq AS interest_id,
    @INTEREST_BOARD_START_ID + seq AS board_id,
    CASE
        WHEN seq < 40 THEN CONCAT('Interest Group ', LPAD(seq + 1, 3, '0'))
        ELSE CONCAT(
            'Interest ',
            LPAD(FLOOR((seq - 40) / 4) + 1, 3, '0'),
            '-',
            MOD(seq - 40, 4) + 1
        )
    END AS name,
    CASE MOD(seq, 5)
        WHEN 0 THEN 'TECH'
        WHEN 1 THEN 'BUSINESS'
        WHEN 2 THEN 'CAREER'
        WHEN 3 THEN 'STUDY'
        ELSE 'CULTURE'
    END AS category,
    CASE
        WHEN seq < 40 THEN NULL
        ELSE @INTEREST_START_ID + FLOOR((seq - 40) / 4)
    END AS parent_id,
    seq + 1 AS display_order,
    TRUE AS is_active,
    NOW() AS created_at,
    NOW() AS updated_at
FROM tmp_seed_seq_10000
WHERE seq < @INTEREST_COUNT;

INSERT INTO interest_major_relation (
    interest_id,
    major_id,
    created_at,
    updated_at
)
SELECT
    interest_id,
    @MAJOR_START_ID + MOD(interest_id - @INTEREST_START_ID, @MAJOR_COUNT) AS major_id,
    NOW() AS created_at,
    NOW() AS updated_at
FROM interests
WHERE interest_id BETWEEN @INTEREST_START_ID AND @INTEREST_END_ID;

INSERT INTO interest_major_relation (
    interest_id,
    major_id,
    created_at,
    updated_at
)
SELECT
    interest_id,
    @MAJOR_START_ID + MOD((interest_id - @INTEREST_START_ID) + 17, @MAJOR_COUNT) AS major_id,
    NOW() AS created_at,
    NOW() AS updated_at
FROM interests
WHERE interest_id BETWEEN @INTEREST_START_ID AND @INTEREST_END_ID;

INSERT INTO interest_major_relation (
    interest_id,
    major_id,
    created_at,
    updated_at
)
SELECT
    interest_id,
    @MAJOR_START_ID + MOD((interest_id - @INTEREST_START_ID) + 43, @MAJOR_COUNT) AS major_id,
    NOW() AS created_at,
    NOW() AS updated_at
FROM interests
WHERE interest_id BETWEEN @INTEREST_START_ID AND @INTEREST_END_ID;

INSERT INTO member_interests (
    member_id,
    interest_id
)
SELECT
    member_id,
    @INTEREST_START_ID + MOD(member_id - @MEMBER_START_ID, @INTEREST_COUNT) AS interest_id
FROM member
WHERE member_id BETWEEN @MEMBER_START_ID AND @MEMBER_END_ID;

INSERT INTO member_interests (
    member_id,
    interest_id
)
SELECT
    member_id,
    @INTEREST_START_ID + MOD((member_id - @MEMBER_START_ID) + 17, @INTEREST_COUNT) AS interest_id
FROM member
WHERE member_id BETWEEN @MEMBER_START_ID AND @MEMBER_END_ID;

ALTER TABLE board AUTO_INCREMENT = 2200201;
ALTER TABLE interests AUTO_INCREMENT = 3200201;

DROP TEMPORARY TABLE IF EXISTS tmp_seed_seq_10000;
