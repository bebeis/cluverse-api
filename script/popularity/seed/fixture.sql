-- V1 전역 기준과 V2 게시판별 기준의 차이를 재현하는 고정 fixture.
SET @member_id = 9100000000;
SET @busy_board_id = 9100000000;
SET @quiet_board_id = 9100000001;

DELETE FROM popular_post WHERE post_id BETWEEN 9100000001 AND 9100000004;

INSERT INTO member (member_id, nickname, status, verification_status, role, created_at, updated_at)
VALUES (@member_id, '__popularity_fixture__', 'ACTIVE', 'APPROVED', 'MEMBER', NOW(), NOW())
ON DUPLICATE KEY UPDATE status = 'ACTIVE', updated_at = NOW();

INSERT INTO board (board_id, board_type, name, description, depth, display_order, is_active, created_at, updated_at)
VALUES
    (@busy_board_id, 'INTEREST', '__popularity_busy__', '측정 전용', 0, 0, TRUE, NOW(), NOW()),
    (@quiet_board_id, 'INTEREST', '__popularity_quiet__', '측정 전용', 0, 0, TRUE, NOW(), NOW())
ON DUPLICATE KEY UPDATE is_active = TRUE, updated_at = NOW();

INSERT INTO post (
    post_id, board_id, member_id, title, content, category, is_anonymous,
    is_pinned, is_external_visible, status, created_at, updated_at
) VALUES
    (9100000001, @busy_board_id, @member_id, 'busy promoted', 'fixture', 'GENERAL', FALSE, FALSE, TRUE, 'ACTIVE', NOW() - INTERVAL 10 MINUTE, NOW()),
    (9100000002, @quiet_board_id, @member_id, 'quiet promoted only by V2', 'fixture', 'GENERAL', FALSE, FALSE, TRUE, 'ACTIVE', NOW() - INTERVAL 10 MINUTE, NOW()),
    (9100000003, @busy_board_id, @member_id, 'busy below threshold', 'fixture', 'GENERAL', FALSE, FALSE, TRUE, 'ACTIVE', NOW() - INTERVAL 10 MINUTE, NOW()),
    (9100000004, @quiet_board_id, @member_id, 'deleted', 'fixture', 'GENERAL', FALSE, FALSE, TRUE, 'DELETED', NOW() - INTERVAL 10 MINUTE, NOW())
ON DUPLICATE KEY UPDATE board_id = VALUES(board_id), status = VALUES(status), created_at = VALUES(created_at), updated_at = NOW();

INSERT INTO post_like_count (post_id, like_count, created_at, updated_at) VALUES
    (9100000001, 35, NOW(), NOW()),
    (9100000002, 7, NOW(), NOW()),
    (9100000003, 33, NOW(), NOW()),
    (9100000004, 100, NOW(), NOW())
ON DUPLICATE KEY UPDATE like_count = VALUES(like_count), updated_at = NOW();

INSERT INTO post_comment_count (post_id, comment_count, created_at, updated_at) VALUES
    (9100000001, 0, NOW(), NOW()),
    (9100000002, 0, NOW(), NOW()),
    (9100000003, 0, NOW(), NOW()),
    (9100000004, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE comment_count = VALUES(comment_count), updated_at = NOW();

INSERT INTO board_popularity_policy (
    board_id, promotion_score, sample_size, policy_source, computed_at, created_at, updated_at
) VALUES
    (@busy_board_id, 100, 1000, 'DISTRIBUTION', NOW(), NOW(), NOW()),
    (@quiet_board_id, 20, 100, 'DISTRIBUTION', NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE promotion_score = VALUES(promotion_score), sample_size = VALUES(sample_size),
    policy_source = VALUES(policy_source), computed_at = NOW(), updated_at = NOW();

SELECT p.post_id, p.board_id, p.status, l.like_count, c.comment_count,
       l.like_count * 3 + c.comment_count * 2 AS score
FROM post p
JOIN post_like_count l ON l.post_id = p.post_id
JOIN post_comment_count c ON c.post_id = p.post_id
WHERE p.post_id BETWEEN 9100000001 AND 9100000004
ORDER BY p.post_id;
