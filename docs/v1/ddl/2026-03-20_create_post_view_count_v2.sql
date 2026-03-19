-- 낙관적 락 기반 조회수 증가 성능 비교용 테이블
-- 목적:
-- 1. 기존 post_view_count(update set ... where) 경로를 유지한다.
-- 2. 낙관적 락 비교 실험은 post_view_count_v2 테이블에서 독립적으로 수행한다.

CREATE TABLE IF NOT EXISTS post_view_count_v2 (
    post_id      BIGINT   NOT NULL COMMENT '→ post.post_id',
    view_count   INT      NOT NULL DEFAULT 0,
    version      BIGINT   NOT NULL DEFAULT 0,
    created_at   DATETIME NOT NULL DEFAULT NOW(),
    updated_at   DATETIME NOT NULL DEFAULT NOW() ON UPDATE NOW(),
    PRIMARY KEY (post_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='게시글 조회수 V2(낙관적 락 비교용)';
