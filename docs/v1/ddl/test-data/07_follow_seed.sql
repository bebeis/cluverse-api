-- ============================================================
-- Follow seed
-- Target: 250,000 follows
-- Dependencies:
--   - Run after 02_member_seed.sql
-- Includes:
--   - follow
-- ============================================================

SET @MEMBER_START_ID = 1000001;
SET @MEMBER_END_ID = 1050000;

DELETE FROM follow
WHERE follower_id BETWEEN @MEMBER_START_ID AND @MEMBER_END_ID
  AND following_id BETWEEN @MEMBER_START_ID AND @MEMBER_END_ID;

INSERT INTO follow (
    follower_id,
    following_id,
    created_at,
    updated_at
)
SELECT
    member_id AS follower_id,
    @MEMBER_START_ID + MOD((member_id - @MEMBER_START_ID) + 7919, 50000) AS following_id,
    NOW() AS created_at,
    NOW() AS updated_at
FROM member
WHERE member_id BETWEEN @MEMBER_START_ID AND @MEMBER_END_ID;

INSERT INTO follow (
    follower_id,
    following_id,
    created_at,
    updated_at
)
SELECT
    member_id AS follower_id,
    @MEMBER_START_ID + MOD((member_id - @MEMBER_START_ID) + 1237, 50000) AS following_id,
    NOW() AS created_at,
    NOW() AS updated_at
FROM member
WHERE member_id BETWEEN @MEMBER_START_ID AND @MEMBER_END_ID;

INSERT INTO follow (
    follower_id,
    following_id,
    created_at,
    updated_at
)
SELECT
    member_id AS follower_id,
    @MEMBER_START_ID + MOD((member_id - @MEMBER_START_ID) + 2089, 50000) AS following_id,
    NOW() AS created_at,
    NOW() AS updated_at
FROM member
WHERE member_id BETWEEN @MEMBER_START_ID AND @MEMBER_END_ID;

INSERT INTO follow (
    follower_id,
    following_id,
    created_at,
    updated_at
)
SELECT
    member_id AS follower_id,
    @MEMBER_START_ID + MOD((member_id - @MEMBER_START_ID) + 3541, 50000) AS following_id,
    NOW() AS created_at,
    NOW() AS updated_at
FROM member
WHERE member_id BETWEEN @MEMBER_START_ID AND @MEMBER_END_ID;

INSERT INTO follow (
    follower_id,
    following_id,
    created_at,
    updated_at
)
SELECT
    member_id AS follower_id,
    @MEMBER_START_ID + MOD((member_id - @MEMBER_START_ID) + 4999, 50000) AS following_id,
    NOW() AS created_at,
    NOW() AS updated_at
FROM member
WHERE member_id BETWEEN @MEMBER_START_ID AND @MEMBER_END_ID;
