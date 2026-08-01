-- 인기글 승격 전용 fixture. 운영/공용 개발 DB에서 실행하지 않는다.
-- 고정 ID 범위: member/board/post 9100000000~9100000005.
-- 초기 기대 승격 집합: 9100000001, 9100000002.

SET @fixture_member_id = 9100000000;
SET @busy_board_id = 9100000000;
SET @quiet_board_id = 9100000001;

DELETE FROM popularity_finalization_claim WHERE post_id BETWEEN 9100000001 AND 9100000005;
DELETE FROM popular_post WHERE post_id BETWEEN 9100000001 AND 9100000005;
DELETE FROM popularity_candidate WHERE post_id BETWEEN 9100000001 AND 9100000005;

INSERT INTO member (
    member_id, nickname, status, verification_status, role, created_at, updated_at
) VALUES (
    @fixture_member_id, '__popularity_fixture__', 'ACTIVE', 'APPROVED', 'MEMBER', NOW(), NOW()
) ON DUPLICATE KEY UPDATE
    status = VALUES(status),
    verification_status = VALUES(verification_status),
    updated_at = NOW();

INSERT INTO board (
    board_id, board_type, name, description, depth, display_order, is_active, created_at, updated_at
) VALUES
    (@busy_board_id, 'INTEREST', '__popularity_busy_fixture__', '인기글 측정 전용', 0, 0, TRUE, NOW(), NOW()),
    (@quiet_board_id, 'INTEREST', '__popularity_quiet_fixture__', '인기글 측정 전용', 0, 0, TRUE, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    is_active = TRUE,
    updated_at = NOW();

INSERT INTO post (
    post_id, board_id, member_id, title, content, category, is_anonymous,
    is_pinned, is_external_visible, status, created_at, updated_at
) VALUES
    (9100000001, @busy_board_id, @fixture_member_id, 'like gate', 'fixture', 'GENERAL', FALSE, FALSE, TRUE, 'ACTIVE', NOW() - INTERVAL 10 MINUTE, NOW()),
    (9100000002, @quiet_board_id, @fixture_member_id, 'comment gate', 'fixture', 'GENERAL', FALSE, FALSE, TRUE, 'ACTIVE', NOW() - INTERVAL 10 MINUTE, NOW()),
    (9100000003, @busy_board_id, @fixture_member_id, 'candidate', 'fixture', 'GENERAL', FALSE, FALSE, TRUE, 'ACTIVE', NOW() - INTERVAL 10 MINUTE, NOW()),
    (9100000004, @busy_board_id, @fixture_member_id, 'score without gate', 'fixture', 'GENERAL', FALSE, FALSE, TRUE, 'ACTIVE', NOW() - INTERVAL 10 MINUTE, NOW()),
    (9100000005, @busy_board_id, @fixture_member_id, 'inactive', 'fixture', 'GENERAL', FALSE, FALSE, TRUE, 'DELETED', NOW() - INTERVAL 10 MINUTE, NOW())
ON DUPLICATE KEY UPDATE
    board_id = VALUES(board_id),
    member_id = VALUES(member_id),
    title = VALUES(title),
    status = VALUES(status),
    created_at = VALUES(created_at),
    updated_at = NOW();

INSERT INTO post_like_count (post_id, like_count, created_at, updated_at) VALUES
    (9100000001, 5, NOW(), NOW()),
    (9100000002, 0, NOW(), NOW()),
    (9100000003, 5, NOW(), NOW()),
    (9100000004, 0, NOW(), NOW()),
    (9100000005, 5, NOW(), NOW())
ON DUPLICATE KEY UPDATE like_count = VALUES(like_count), updated_at = NOW();

INSERT INTO post_comment_count (post_id, comment_count, created_at, updated_at) VALUES
    (9100000001, 0, NOW(), NOW()),
    (9100000002, 1, NOW(), NOW()),
    (9100000003, 0, NOW(), NOW()),
    (9100000004, 0, NOW(), NOW()),
    (9100000005, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE comment_count = VALUES(comment_count), updated_at = NOW();

INSERT INTO post_view_count (post_id, view_count, created_at, updated_at) VALUES
    (9100000001, 85, NOW(), NOW()),
    (9100000002, 48, NOW(), NOW()),
    (9100000003, 84, NOW(), NOW()),
    (9100000004, 120, NOW(), NOW()),
    (9100000005, 85, NOW(), NOW())
ON DUPLICATE KEY UPDATE view_count = VALUES(view_count), updated_at = NOW();

INSERT INTO board_popularity_policy (
    board_id, promotion_score, like_gate, comment_gate, sample_size,
    policy_source, computed_at, created_at, updated_at
) VALUES
    (@busy_board_id, 100, 5, 3, 0, 'DEFAULT', NOW(), NOW(), NOW()),
    (@quiet_board_id, 50, 2, 1, 0, 'DEFAULT', NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE
    promotion_score = VALUES(promotion_score),
    like_gate = VALUES(like_gate),
    comment_gate = VALUES(comment_gate),
    sample_size = VALUES(sample_size),
    policy_source = VALUES(policy_source),
    computed_at = NOW(),
    updated_at = NOW();

SELECT p.post_id,
       p.board_id,
       p.status,
       plc.like_count,
       pcc.comment_count,
       pvc.view_count,
       plc.like_count * 3 + pcc.comment_count * 2 + pvc.view_count AS score
FROM post p
JOIN post_like_count plc ON plc.post_id = p.post_id
JOIN post_comment_count pcc ON pcc.post_id = p.post_id
JOIN post_view_count pvc ON pvc.post_id = p.post_id
WHERE p.post_id BETWEEN 9100000001 AND 9100000005
ORDER BY p.post_id;
