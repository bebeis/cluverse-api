-- ============================================================
-- Major seed
-- Target:
--   - major: 300 rows
--   - member_major: about 67,500 rows
-- Dependencies:
--   - Run after 02_member_seed.sql
-- Includes:
--   - board (major range only)
--   - major
--   - member_major
-- ============================================================

SET @MAJOR_BOARD_START_ID = 2100001;
SET @MAJOR_BOARD_COUNT = 300;
SET @MAJOR_BOARD_END_ID = @MAJOR_BOARD_START_ID + @MAJOR_BOARD_COUNT - 1;
SET @MAJOR_START_ID = 3100001;
SET @MAJOR_COUNT = 300;
SET @MAJOR_END_ID = @MAJOR_START_ID + @MAJOR_COUNT - 1;
SET @MEMBER_START_ID = 1000001;
SET @MEMBER_END_ID = 1050000;

DELETE FROM member_major
WHERE member_id BETWEEN @MEMBER_START_ID AND @MEMBER_END_ID
  AND major_id BETWEEN @MAJOR_START_ID AND @MAJOR_END_ID;

DELETE FROM major
WHERE major_id BETWEEN @MAJOR_START_ID AND @MAJOR_END_ID;

DELETE FROM board
WHERE board_id BETWEEN @MAJOR_BOARD_START_ID AND @MAJOR_BOARD_END_ID;

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
    @MAJOR_BOARD_START_ID + seq AS board_id,
    'DEPARTMENT' AS board_type,
    CASE
        WHEN seq < 60 THEN CONCAT('Major Group Board ', LPAD(seq + 1, 3, '0'))
        ELSE CONCAT(
            'Major Board ',
            LPAD(FLOOR((seq - 60) / 4) + 1, 3, '0'),
            '-',
            MOD(seq - 60, 4) + 1
        )
    END AS name,
    'Generated seed board for major domain' AS description,
    NULL AS parent_id,
    CASE
        WHEN seq < 60 THEN 0
        ELSE 1
    END AS depth,
    seq + 1 AS display_order,
    TRUE AS is_active,
    NOW() AS created_at,
    NOW() AS updated_at
FROM tmp_seed_seq_10000
WHERE seq < @MAJOR_COUNT;

INSERT INTO major (
    major_id,
    board_id,
    name,
    parent_id,
    depth,
    display_order,
    is_active,
    created_at,
    updated_at
)
SELECT
    @MAJOR_START_ID + seq AS major_id,
    @MAJOR_BOARD_START_ID + seq AS board_id,
    CASE
        WHEN seq < 60 THEN CONCAT('Major Group ', LPAD(seq + 1, 3, '0'))
        ELSE CONCAT(
            'Major ',
            LPAD(FLOOR((seq - 60) / 4) + 1, 3, '0'),
            '-',
            MOD(seq - 60, 4) + 1
        )
    END AS name,
    CASE
        WHEN seq < 60 THEN NULL
        ELSE @MAJOR_START_ID + FLOOR((seq - 60) / 4)
    END AS parent_id,
    CASE
        WHEN seq < 60 THEN 0
        ELSE 1
    END AS depth,
    seq + 1 AS display_order,
    TRUE AS is_active,
    NOW() AS created_at,
    NOW() AS updated_at
FROM tmp_seed_seq_10000
WHERE seq < @MAJOR_COUNT;

INSERT INTO member_major (
    member_id,
    major_id,
    major_type,
    created_at,
    updated_at
)
SELECT
    member_id,
    @MAJOR_START_ID + MOD(member_id - @MEMBER_START_ID, @MAJOR_COUNT) AS major_id,
    'PRIMARY' AS major_type,
    NOW() AS created_at,
    NOW() AS updated_at
FROM member
WHERE member_id BETWEEN @MEMBER_START_ID AND @MEMBER_END_ID;

INSERT INTO member_major (
    member_id,
    major_id,
    major_type,
    created_at,
    updated_at
)
SELECT
    member_id,
    @MAJOR_START_ID + MOD((member_id - @MEMBER_START_ID) + 7, @MAJOR_COUNT) AS major_id,
    'DOUBLE_MAJOR' AS major_type,
    NOW() AS created_at,
    NOW() AS updated_at
FROM member
WHERE member_id BETWEEN @MEMBER_START_ID AND @MEMBER_END_ID
  AND MOD(member_id, 4) = 0;

INSERT INTO member_major (
    member_id,
    major_id,
    major_type,
    created_at,
    updated_at
)
SELECT
    member_id,
    @MAJOR_START_ID + MOD((member_id - @MEMBER_START_ID) + 13, @MAJOR_COUNT) AS major_id,
    'MINOR' AS major_type,
    NOW() AS created_at,
    NOW() AS updated_at
FROM member
WHERE member_id BETWEEN @MEMBER_START_ID AND @MEMBER_END_ID
  AND MOD(member_id, 10) = 0;

ALTER TABLE board AUTO_INCREMENT = 2100301;
ALTER TABLE major AUTO_INCREMENT = 3100301;

DROP TEMPORARY TABLE IF EXISTS tmp_seed_seq_10000;
