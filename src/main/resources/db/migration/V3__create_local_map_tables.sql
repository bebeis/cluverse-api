-- 로컬맵 장소 원본. 네이버 지역 검색은 안정적인 장소 ID를 제공하지 않으므로
-- 정규화한 장소 속성의 SHA-256 fingerprint를 공급자 내부 식별자로 사용한다.
CREATE TABLE place (
    place_id            BIGINT        NOT NULL AUTO_INCREMENT,
    provider            VARCHAR(20)   NOT NULL,
    source_fingerprint  CHAR(64)      NOT NULL,
    name                VARCHAR(200)  NOT NULL,
    category            VARCHAR(20)   NOT NULL,
    raw_category        VARCHAR(200)  NULL,
    address             VARCHAR(300)  NULL,
    road_address        VARCHAR(300)  NULL,
    latitude            DECIMAL(10,7) NOT NULL,
    longitude           DECIMAL(10,7) NOT NULL,
    source_url          VARCHAR(500)  NULL,
    synchronized_at     DATETIME      NOT NULL,
    created_at          DATETIME      NOT NULL DEFAULT NOW(),
    updated_at          DATETIME      NOT NULL DEFAULT NOW() ON UPDATE NOW(),
    PRIMARY KEY (place_id),
    UNIQUE KEY uk_place_provider_fingerprint (provider, source_fingerprint)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='외부 장소 검색 결과를 정규화한 장소';

CREATE TABLE university_campus (
    university_campus_id BIGINT        NOT NULL AUTO_INCREMENT,
    university_id        BIGINT        NOT NULL,
    name                 VARCHAR(100)  NOT NULL,
    latitude             DECIMAL(10,7) NOT NULL,
    longitude            DECIMAL(10,7) NOT NULL,
    local_radius_meter   INT           NOT NULL,
    is_active            BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at           DATETIME      NOT NULL DEFAULT NOW(),
    updated_at           DATETIME      NOT NULL DEFAULT NOW() ON UPDATE NOW(),
    PRIMARY KEY (university_campus_id),
    UNIQUE KEY uk_university_campus_name (university_id, name),
    KEY idx_university_campus_active (university_id, is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='학교별 로컬맵 판정 기준 캠퍼스';

CREATE TABLE post_place (
    post_place_id        BIGINT      NOT NULL AUTO_INCREMENT,
    post_id              BIGINT      NOT NULL,
    place_id             BIGINT      NOT NULL,
    display_order        TINYINT     NOT NULL,
    author_university_id BIGINT      NULL,
    university_campus_id BIGINT      NULL,
    recommended          BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at           DATETIME    NOT NULL DEFAULT NOW(),
    updated_at           DATETIME    NOT NULL DEFAULT NOW() ON UPDATE NOW(),
    PRIMARY KEY (post_place_id),
    UNIQUE KEY uk_post_place (post_id, place_id),
    UNIQUE KEY uk_post_place_order (post_id, display_order),
    KEY idx_post_place_local_map (author_university_id, university_campus_id, recommended, place_id),
    KEY idx_post_place_place (place_id, post_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='게시글에 첨부된 장소와 작성 당시 로컬 판정';

CREATE TABLE comment_place (
    comment_place_id     BIGINT      NOT NULL AUTO_INCREMENT,
    comment_id           BIGINT      NOT NULL,
    place_id             BIGINT      NOT NULL,
    author_university_id BIGINT      NULL,
    university_campus_id BIGINT      NULL,
    recommended          BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at           DATETIME    NOT NULL DEFAULT NOW(),
    updated_at           DATETIME    NOT NULL DEFAULT NOW() ON UPDATE NOW(),
    PRIMARY KEY (comment_place_id),
    UNIQUE KEY uk_comment_place_comment (comment_id),
    KEY idx_comment_place_local_map (author_university_id, university_campus_id, recommended, place_id),
    KEY idx_comment_place_place (place_id, comment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='댓글에 첨부된 장소와 작성 당시 로컬 판정';

ALTER TABLE post
    ADD COLUMN client_request_id CHAR(36) NULL AFTER client_ip,
    ADD UNIQUE KEY uk_post_member_request (member_id, client_request_id);

ALTER TABLE comment
    ADD COLUMN client_request_id CHAR(36) NULL AFTER client_ip,
    ADD UNIQUE KEY uk_comment_member_request (member_id, client_request_id);
