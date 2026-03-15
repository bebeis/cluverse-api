-- 게시글 좋아요/댓글/북마크 수 분리 마이그레이션
-- 대상: 이미 운영 중인 MySQL DB
-- 주의:
-- 1. 애플리케이션 배포와 같은 타이밍에 적용해야 합니다.
-- 2. 0건 게시글은 count 테이블에 row를 만들지 않습니다.
-- 3. 대용량 post 테이블이라면 ALTER TABLE은 점검 시간 또는 online schema change 도구 적용을 권장합니다.

-- 1. 집계 테이블 생성
CREATE TABLE IF NOT EXISTS post_like_count (
    post_id      BIGINT   NOT NULL COMMENT '→ post.post_id',
    like_count   INT      NOT NULL DEFAULT 0,
    created_at   DATETIME NOT NULL DEFAULT NOW(),
    updated_at   DATETIME NOT NULL DEFAULT NOW() ON UPDATE NOW(),
    PRIMARY KEY (post_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='게시글 좋아요 수';

CREATE TABLE IF NOT EXISTS post_comment_count (
    post_id        BIGINT   NOT NULL COMMENT '→ post.post_id',
    comment_count  INT      NOT NULL DEFAULT 0,
    created_at     DATETIME NOT NULL DEFAULT NOW(),
    updated_at     DATETIME NOT NULL DEFAULT NOW() ON UPDATE NOW(),
    PRIMARY KEY (post_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='게시글 댓글 수';

CREATE TABLE IF NOT EXISTS post_bookmark_count (
    post_id          BIGINT   NOT NULL COMMENT '→ post.post_id',
    bookmark_count   INT      NOT NULL DEFAULT 0,
    created_at       DATETIME NOT NULL DEFAULT NOW(),
    updated_at       DATETIME NOT NULL DEFAULT NOW() ON UPDATE NOW(),
    PRIMARY KEY (post_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='게시글 북마크 수';

-- -- 2. 기존 post 테이블의 집계값 이관
-- INSERT INTO post_like_count (post_id, like_count, created_at, updated_at)
-- SELECT post_id, like_count, NOW(), NOW()
-- FROM post
-- WHERE like_count > 0
-- ON DUPLICATE KEY UPDATE
--     like_count = VALUES(like_count),
--     updated_at = NOW();

-- INSERT INTO post_comment_count (post_id, comment_count, created_at, updated_at)
-- SELECT post_id, comment_count, NOW(), NOW()
-- FROM post
-- WHERE comment_count > 0
-- ON DUPLICATE KEY UPDATE
--     comment_count = VALUES(comment_count),
--     updated_at = NOW();

-- INSERT INTO post_bookmark_count (post_id, bookmark_count, created_at, updated_at)
-- SELECT post_id, bookmark_count, NOW(), NOW()
-- FROM post
-- WHERE bookmark_count > 0
-- ON DUPLICATE KEY UPDATE
--     bookmark_count = VALUES(bookmark_count),
--     updated_at = NOW();

-- 3. 검증 쿼리 예시
-- SELECT COUNT(*) FROM post WHERE like_count > 0;
-- SELECT COUNT(*) FROM post_like_count;
-- SELECT COUNT(*) FROM post WHERE comment_count > 0;
-- SELECT COUNT(*) FROM post_comment_count;
-- SELECT COUNT(*) FROM post WHERE bookmark_count > 0;
-- SELECT COUNT(*) FROM post_bookmark_count;

-- 4. 애플리케이션이 새 테이블을 사용하도록 배포한 뒤 기존 컬럼 제거
ALTER TABLE post
    DROP COLUMN like_count,
    DROP COLUMN comment_count,
    DROP COLUMN bookmark_count;
