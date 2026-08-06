-- devlog-6 인라인 판정 부가 비용 측정 전용 fixture.
-- disabled/enabled 조건과 like/comment 경로가 서로 다른 게시글을 사용한다.
SET @member_id = 9200000000;
SET @board_id = 9200000000;
SET @posts_per_group = 1000;
SET @post_id_min = 9200000001;
SET @post_id_max = 9200004000;

DELETE FROM post_comment_activity WHERE post_id BETWEEN @post_id_min AND @post_id_max;
DELETE comment_place
FROM comment_place
JOIN comment ON comment.comment_id = comment_place.comment_id
WHERE comment.post_id BETWEEN @post_id_min AND @post_id_max;
DELETE comment_like
FROM comment_like
JOIN comment ON comment.comment_id = comment_like.comment_id
WHERE comment.post_id BETWEEN @post_id_min AND @post_id_max;
DELETE FROM comment WHERE post_id BETWEEN @post_id_min AND @post_id_max;
DELETE FROM popular_post WHERE post_id BETWEEN @post_id_min AND @post_id_max;
DELETE FROM post_like WHERE post_id BETWEEN @post_id_min AND @post_id_max;
DELETE FROM bookmark WHERE post_id BETWEEN @post_id_min AND @post_id_max;
DELETE FROM post_place WHERE post_id BETWEEN @post_id_min AND @post_id_max;
DELETE FROM post_tag WHERE post_id BETWEEN @post_id_min AND @post_id_max;
DELETE FROM post_image WHERE post_id BETWEEN @post_id_min AND @post_id_max;
DELETE FROM post_view_count WHERE post_id BETWEEN @post_id_min AND @post_id_max;
DELETE FROM post_like_count WHERE post_id BETWEEN @post_id_min AND @post_id_max;
DELETE FROM post_comment_count WHERE post_id BETWEEN @post_id_min AND @post_id_max;
DELETE FROM post_bookmark_count WHERE post_id BETWEEN @post_id_min AND @post_id_max;
DELETE FROM post WHERE post_id BETWEEN @post_id_min AND @post_id_max;

INSERT INTO member (member_id, nickname, status, verification_status, role, created_at, updated_at)
VALUES (@member_id, '__popularity_inline_benchmark__', 'ACTIVE', 'APPROVED', 'MEMBER', NOW(), NOW())
ON DUPLICATE KEY UPDATE status = 'ACTIVE', updated_at = NOW();

INSERT INTO board (
    board_id, board_type, name, description, depth, display_order,
    is_active, created_at, updated_at
)
VALUES (
    @board_id, 'INTEREST', '__popularity_inline_benchmark__', '측정 전용', 0, 0,
    TRUE, NOW(), NOW()
)
ON DUPLICATE KEY UPDATE is_active = TRUE, updated_at = NOW();

-- 측정 중에는 승격 UPSERT가 발생하지 않도록 임계값을 높게 고정한다.
INSERT INTO board_popularity_policy (
    board_id, promotion_score, sample_size, policy_source,
    computed_at, created_at, updated_at
)
VALUES (@board_id, 1000000, 4000, 'DISTRIBUTION', NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE
    promotion_score = VALUES(promotion_score),
    sample_size = VALUES(sample_size),
    policy_source = VALUES(policy_source),
    computed_at = NOW(),
    updated_at = NOW();

DROP TEMPORARY TABLE IF EXISTS tmp_popularity_inline_seq;
CREATE TEMPORARY TABLE tmp_popularity_inline_seq
ENGINE=MEMORY
AS
SELECT ones.digit
     + tens.digit * 10
     + hundreds.digit * 100 AS seq
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
) hundreds;

ALTER TABLE tmp_popularity_inline_seq ADD PRIMARY KEY (seq);

INSERT INTO post (
    post_id, board_id, member_id, title, content, category,
    is_anonymous, is_pinned, is_external_visible, status,
    client_ip, created_at, updated_at
)
SELECT
    fixture.start_id + seq.seq,
    @board_id,
    @member_id,
    CONCAT('popularity inline ', fixture.label, ' ', LPAD(seq.seq + 1, 4, '0')),
    '측정 전용 게시글',
    'GENERAL',
    FALSE, FALSE, TRUE, 'ACTIVE',
    'benchmark-popularity-inline',
    NOW() - INTERVAL MOD(seq.seq, 24) HOUR,
    NOW()
FROM tmp_popularity_inline_seq seq
CROSS JOIN (
    SELECT 9200000001 AS start_id, 'disabled-like' AS label
    UNION ALL SELECT 9200001001, 'enabled-like'
    UNION ALL SELECT 9200002001, 'disabled-comment'
    UNION ALL SELECT 9200003001, 'enabled-comment'
) fixture
WHERE seq.seq < @posts_per_group;

SELECT
    CASE
        WHEN post_id BETWEEN 9200000001 AND 9200001000 THEN 'disabled-like'
        WHEN post_id BETWEEN 9200001001 AND 9200002000 THEN 'enabled-like'
        WHEN post_id BETWEEN 9200002001 AND 9200003000 THEN 'disabled-comment'
        ELSE 'enabled-comment'
    END AS fixture_group,
    COUNT(*) AS post_count
FROM post
WHERE post_id BETWEEN @post_id_min AND @post_id_max
GROUP BY fixture_group
ORDER BY fixture_group;
