-- ============================================================
-- Block seed
-- Target: 25,000 blocks
-- Dependencies:
--   - Run after 02_member_seed.sql
-- Includes:
--   - block
-- ============================================================

SET @MEMBER_START_ID = 1000001;
SET @MEMBER_END_ID = 1050000;

DELETE FROM block
WHERE blocker_id BETWEEN @MEMBER_START_ID AND @MEMBER_END_ID
  AND blocked_id BETWEEN @MEMBER_START_ID AND @MEMBER_END_ID;

INSERT INTO block (
    blocker_id,
    blocked_id,
    created_at,
    updated_at
)
SELECT
    member_id AS blocker_id,
    @MEMBER_START_ID + MOD((member_id - @MEMBER_START_ID) + 12345, 50000) AS blocked_id,
    NOW() AS created_at,
    NOW() AS updated_at
FROM member
WHERE member_id BETWEEN @MEMBER_START_ID AND @MEMBER_END_ID
  AND MOD(member_id - @MEMBER_START_ID, 2) = 0;
