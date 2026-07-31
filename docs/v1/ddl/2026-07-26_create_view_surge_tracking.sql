-- 2026-07-26: 조회수 급상승 추적 테이블 생성 (조회수 증가 API V4 — devlog #5)
-- 급상승 감지 시 UPSERT되고, 각 인스턴스가 expires_at > NOW() 행을
-- 주기적으로 읽어 해당 게시글의 조회수 쓰기를 Redis write-back 경로로 라우팅한다.
-- V1__init_schema.sql baseline에도 동일 DDL 반영됨.

CREATE TABLE IF NOT EXISTS view_surge_tracking (
    post_id      BIGINT   NOT NULL COMMENT '→ post.post_id',
    activated_at DATETIME NOT NULL,
    expires_at   DATETIME NOT NULL,
    PRIMARY KEY (post_id),
    INDEX idx_expires_at (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='조회수 급상승 추적 (V4 Redis Write-back)';
