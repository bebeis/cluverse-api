-- ============================================================
-- Cluverse DDL v1.1
-- MySQL 8.x
-- 외래키 제약조건 없음 | 인덱스 미적용
-- ============================================================

-- ------------------------------------------------------------
-- 1. 회원/인증 도메인
-- ------------------------------------------------------------

-- 1.1 member (회원)
CREATE TABLE member (
    member_id    BIGINT       NOT NULL AUTO_INCREMENT,
    nickname     VARCHAR(50)  NOT NULL,
    university_id BIGINT      NULL                              COMMENT '소속 학교 → university.university_id, 소셜 가입 직후에는 NULL 가능',
    status       VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE'         COMMENT 'ACTIVE / SUSPENDED / BANNED / DELETED',
    verification_status VARCHAR(20) NOT NULL DEFAULT 'NONE'     COMMENT 'NONE / PENDING / APPROVED / REJECTED',
    verification_rejected_reason VARCHAR(50) NULL               COMMENT '인증 거절 사유 (범주형 코드)',
    role         VARCHAR(20)  NOT NULL DEFAULT 'MEMBER'         COMMENT 'MEMBER / MODERATOR / ADMIN',
    last_login_at DATETIME    NULL,
    source_system VARCHAR(30)  NULL                              COMMENT 'WEB_ADMIN / WEB_USER / MOBILE_APP / API / BATCH 등',
    client_ip    VARCHAR(45)  NULL                              COMMENT '요청 IP',
    created_at   DATETIME     NOT NULL DEFAULT NOW(),
    updated_at   DATETIME     NOT NULL DEFAULT NOW() ON UPDATE NOW(),
    PRIMARY KEY (member_id),
    UNIQUE KEY uk_member_nickname (nickname)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='회원';

-- 1.2 member_auth (회원 인증 정보) — Shared PK
CREATE TABLE member_auth (
    member_id     BIGINT       NOT NULL                         COMMENT 'Shared PK → member.member_id',
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NULL                             COMMENT '소셜 로그인 전용이면 NULL',
    created_at    DATETIME     NOT NULL DEFAULT NOW(),
    updated_at    DATETIME     NOT NULL DEFAULT NOW() ON UPDATE NOW(),
    PRIMARY KEY (member_id),
    UNIQUE KEY uk_member_auth_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='회원 인증 정보';

-- 1.3 member_status_history (회원 상태 이력)
CREATE TABLE member_status_history (
    member_status_history_id BIGINT      NOT NULL AUTO_INCREMENT,
    member_id                BIGINT      NOT NULL               COMMENT '대상 회원 → member.member_id',
    previous_status          VARCHAR(20) NOT NULL,
    new_status               VARCHAR(20) NOT NULL,
    change_type              VARCHAR(30) NOT NULL               COMMENT 'SIGNUP / SUSPEND / BAN / UNSUSPEND / DELETE 등',
    change_reason            TEXT        NULL                   COMMENT '변경 사유 (운영자 메모)',
    changed_by               BIGINT      NULL                  COMMENT '변경 수행자 → member.member_id (시스템이면 NULL)',
    source_system            VARCHAR(30) NULL                  COMMENT 'WEB_ADMIN / API / BATCH 등',
    client_ip                VARCHAR(45) NULL                  COMMENT '요청 IP',
    created_at               DATETIME    NOT NULL DEFAULT NOW(),
    updated_at               DATETIME    NOT NULL DEFAULT NOW() ON UPDATE NOW(),
    PRIMARY KEY (member_status_history_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='회원 상태 이력';

-- 1.4 member_credential (인증 자료)
CREATE TABLE member_credential (
    member_credential_id BIGINT       NOT NULL AUTO_INCREMENT,
    member_id            BIGINT       NOT NULL                  COMMENT '→ member.member_id',
    credential_type      VARCHAR(30)  NOT NULL                  COMMENT 'SCHOOL_EMAIL / STUDENT_ID_CARD / ENROLLMENT_CERT',
    credential_value     VARCHAR(500) NOT NULL                  COMMENT '이메일 주소 또는 파일 URL',
    status               VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING / APPROVED / REJECTED',
    rejected_reason      VARCHAR(50)  NULL                      COMMENT '거절 사유 코드',
    reviewed_by          BIGINT       NULL                      COMMENT '검수 운영자 → member.member_id',
    reviewed_at          DATETIME     NULL,
    source_system        VARCHAR(30)  NULL                      COMMENT 'WEB_ADMIN / API / BATCH 등',
    client_ip            VARCHAR(45)  NULL                      COMMENT '요청 IP',
    created_at           DATETIME     NOT NULL DEFAULT NOW(),
    updated_at           DATETIME     NOT NULL DEFAULT NOW() ON UPDATE NOW(),
    PRIMARY KEY (member_credential_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='회원 인증 자료';

-- 1.5 member_profile (프로필) — Shared PK
CREATE TABLE member_profile (
    member_id         BIGINT       NOT NULL                     COMMENT 'Shared PK → member.member_id',
    bio               TEXT         NULL,
    profile_image_url VARCHAR(500) NULL,
    link_github       VARCHAR(500) NULL,
    link_notion       VARCHAR(500) NULL,
    link_portfolio    VARCHAR(500) NULL,
    link_instagram    VARCHAR(500) NULL,
    link_etc          VARCHAR(500) NULL,
    is_public         BOOLEAN      NOT NULL DEFAULT TRUE        COMMENT '프로필 전체 공개 여부',
    visible_fields    JSON         NULL                         COMMENT '필드별 공개 설정',
    created_at        DATETIME     NOT NULL DEFAULT NOW(),
    updated_at        DATETIME     NOT NULL DEFAULT NOW() ON UPDATE NOW(),
    PRIMARY KEY (member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='회원 프로필';

-- 1.6 social_account (소셜 로그인 연동)
CREATE TABLE social_account (
    social_account_id BIGINT       NOT NULL AUTO_INCREMENT,
    member_id         BIGINT       NOT NULL                     COMMENT '→ member.member_id',
    provider          VARCHAR(30)  NOT NULL                     COMMENT 'GOOGLE / KAKAO / NAVER / APPLE',
    provider_user_id  VARCHAR(255) NOT NULL,
    created_at        DATETIME     NOT NULL DEFAULT NOW(),
    updated_at        DATETIME     NOT NULL DEFAULT NOW() ON UPDATE NOW(),
    PRIMARY KEY (social_account_id),
    UNIQUE KEY uk_social_provider_user (provider, provider_user_id),
    UNIQUE KEY uk_social_member_provider (member_id, provider)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='소셜 로그인 연동';

-- 1.7 terms (약관 마스터)
CREATE TABLE terms (
    terms_id     BIGINT       NOT NULL AUTO_INCREMENT,
    terms_type   VARCHAR(30)  NOT NULL                          COMMENT 'SERVICE / PRIVACY / MARKETING 등',
    title        VARCHAR(200) NOT NULL,
    content      TEXT         NOT NULL,
    version      VARCHAR(20)  NOT NULL,
    is_required  BOOLEAN      NOT NULL DEFAULT TRUE,
    is_active    BOOLEAN      NOT NULL DEFAULT TRUE,
    effective_at DATETIME     NOT NULL,
    created_at   DATETIME     NOT NULL DEFAULT NOW(),
    updated_at   DATETIME     NOT NULL DEFAULT NOW() ON UPDATE NOW(),
    PRIMARY KEY (terms_id),
    UNIQUE KEY uk_terms_type_version (terms_type, version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='약관 마스터';

-- 1.8 member_terms_agreement (회원-약관 동의 이력)
CREATE TABLE member_terms_agreement (
    member_terms_agreement_id BIGINT   NOT NULL AUTO_INCREMENT,
    member_id                 BIGINT   NOT NULL                 COMMENT '→ member.member_id',
    terms_id                  BIGINT   NOT NULL                 COMMENT '→ terms.terms_id',
    agreed_at                 DATETIME NOT NULL,
    created_at                DATETIME NOT NULL DEFAULT NOW(),
    updated_at                DATETIME NOT NULL DEFAULT NOW() ON UPDATE NOW(),
    PRIMARY KEY (member_terms_agreement_id),
    UNIQUE KEY uk_member_terms (member_id, terms_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='회원-약관 동의 이력';


-- ------------------------------------------------------------
-- 2. 학교/학과/관심사 도메인
-- ------------------------------------------------------------

-- 2.1 university (대학교 마스터)
CREATE TABLE university (
    university_id   BIGINT       NOT NULL AUTO_INCREMENT,
    name            VARCHAR(100) NOT NULL,
    email_domain    VARCHAR(100) NULL                           COMMENT '학교 이메일 도메인 (예: snu.ac.kr)',
    badge_image_url VARCHAR(500) NULL,
    address         VARCHAR(300) NULL,
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      DATETIME     NOT NULL DEFAULT NOW(),
    updated_at      DATETIME     NOT NULL DEFAULT NOW() ON UPDATE NOW(),
    PRIMARY KEY (university_id),
    UNIQUE KEY uk_university_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='대학교 마스터';

-- 2.2 major (학과 마스터) — board 상속 (board_id 보유)
CREATE TABLE major (
    major_id      BIGINT       NOT NULL AUTO_INCREMENT,
    board_id      BIGINT       NOT NULL                        COMMENT '1:1 → board.board_id',
    name          VARCHAR(100) NOT NULL,
    parent_id     BIGINT       NULL                            COMMENT '상위 학과 그룹 → major.major_id',
    depth         TINYINT      NOT NULL DEFAULT 0              COMMENT '0=그룹, 1=세부학과',
    display_order INT          NOT NULL DEFAULT 0,
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    DATETIME     NOT NULL DEFAULT NOW(),
    updated_at    DATETIME     NOT NULL DEFAULT NOW() ON UPDATE NOW(),
    PRIMARY KEY (major_id),
    UNIQUE KEY uk_major_board (board_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='학과 마스터';

-- 2.3 member_major (회원-학과 매핑)
CREATE TABLE member_major (
    member_major_id BIGINT      NOT NULL AUTO_INCREMENT,
    member_id       BIGINT      NOT NULL                       COMMENT '→ member.member_id',
    major_id        BIGINT      NOT NULL                       COMMENT '→ major.major_id',
    major_type      VARCHAR(20) NOT NULL                       COMMENT 'PRIMARY / DOUBLE_MAJOR / MINOR',
    created_at      DATETIME    NOT NULL DEFAULT NOW(),
    updated_at      DATETIME    NOT NULL DEFAULT NOW() ON UPDATE NOW(),
    PRIMARY KEY (member_major_id),
    UNIQUE KEY uk_member_major (member_id, major_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='회원-학과 매핑';

-- 2.4 interests (관심 태그 마스터) — board 상속 (board_id 보유)
CREATE TABLE interests (
    interest_id   BIGINT      NOT NULL AUTO_INCREMENT,
    board_id      BIGINT      NOT NULL                         COMMENT '1:1 → board.board_id',
    name          VARCHAR(50) NOT NULL,
    category      VARCHAR(50) NULL                             COMMENT '상위 카테고리 (기술, 비즈니스, 문화 등)',
    parent_id     BIGINT      NULL                             COMMENT '상위 태그 → interests.interest_id',
    display_order INT         NOT NULL DEFAULT 0,
    is_active     BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at    DATETIME    NOT NULL DEFAULT NOW(),
    updated_at    DATETIME    NOT NULL DEFAULT NOW() ON UPDATE NOW(),
    PRIMARY KEY (interest_id),
    UNIQUE KEY uk_interests_board (board_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='관심 태그 마스터';

-- 2.5 interest_major_relation (관심태그-학과 연관)
CREATE TABLE interest_major_relation (
    interest_major_relation_id BIGINT   NOT NULL AUTO_INCREMENT,
    interest_id                BIGINT   NOT NULL               COMMENT '→ interests.interest_id',
    major_id                   BIGINT   NOT NULL               COMMENT '→ major.major_id',
    created_at                 DATETIME NOT NULL DEFAULT NOW(),
    updated_at                 DATETIME NOT NULL DEFAULT NOW() ON UPDATE NOW(),
    PRIMARY KEY (interest_major_relation_id),
    UNIQUE KEY uk_interest_major (interest_id, major_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='관심태그-학과 연관';

-- 2.6 member_interests (회원-관심태그 매핑)
CREATE TABLE member_interests (
    member_id          BIGINT   NOT NULL                       COMMENT '→ member.member_id',
    interest_id        BIGINT   NOT NULL                       COMMENT '→ interests.interest_id',
    PRIMARY KEY (member_id, interest_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='회원-관심태그 매핑';


-- ------------------------------------------------------------
-- 3. 보드/게시글/댓글 도메인
-- ------------------------------------------------------------

-- 3.1 board (보드 — 슈퍼타입)
CREATE TABLE board (
    board_id      BIGINT       NOT NULL AUTO_INCREMENT,
    board_type    VARCHAR(20)  NOT NULL                        COMMENT 'DEPARTMENT / INTEREST / GROUP',
    name          VARCHAR(100) NOT NULL,
    description   TEXT         NULL,
    parent_id     BIGINT       NULL                            COMMENT '상위 보드 → board.board_id (최대 3-depth)',
    depth         TINYINT      NOT NULL DEFAULT 0              COMMENT '0, 1, 2',
    display_order INT          NOT NULL DEFAULT 0,
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    DATETIME     NOT NULL DEFAULT NOW(),
    updated_at    DATETIME     NOT NULL DEFAULT NOW() ON UPDATE NOW(),
    PRIMARY KEY (board_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='보드 (슈퍼타입)';

-- 3.2 post (게시글)
CREATE TABLE post (
    post_id             BIGINT       NOT NULL AUTO_INCREMENT,
    board_id            BIGINT       NOT NULL                  COMMENT '→ board.board_id',
    member_id           BIGINT       NOT NULL                  COMMENT '작성자 → member.member_id',
    title               VARCHAR(200) NOT NULL,
    content             TEXT         NOT NULL,
    category            VARCHAR(30)  NULL                      COMMENT '질문/정보/후기/자료/모집 등',
    is_anonymous        BOOLEAN      NOT NULL DEFAULT FALSE,
    is_pinned           BOOLEAN      NOT NULL DEFAULT FALSE,
    is_external_visible BOOLEAN      NOT NULL DEFAULT TRUE     COMMENT '외부 공개 여부 (그룹 게시판용)',
    status              VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE / BLINDED / DELETED',
    view_count          INT          NOT NULL DEFAULT 0,
    like_count          INT          NOT NULL DEFAULT 0        COMMENT '반정규화',
    comment_count       INT          NOT NULL DEFAULT 0        COMMENT '반정규화',
    bookmark_count      INT          NOT NULL DEFAULT 0        COMMENT '반정규화',
    deleted_at          DATETIME     NULL,
    client_ip           VARCHAR(45)  NULL                      COMMENT '작성자 IP (분쟁/법적 대응)',
    created_at          DATETIME     NOT NULL DEFAULT NOW(),
    updated_at          DATETIME     NOT NULL DEFAULT NOW() ON UPDATE NOW(),
    PRIMARY KEY (post_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='게시글';

-- 3.3 post_image (게시글 이미지)
CREATE TABLE post_image (
    post_image_id BIGINT       NOT NULL AUTO_INCREMENT,
    post_id       BIGINT       NOT NULL                        COMMENT '→ post.post_id',
    image_url     VARCHAR(500) NOT NULL,
    display_order TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (post_image_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='게시글 이미지';

-- 3.4 post_tag (게시글-태그 매핑)
CREATE TABLE post_tag (
    post_id  BIGINT      NOT NULL                              COMMENT '→ post.post_id',
    tag_name VARCHAR(50) NOT NULL,
    PRIMARY KEY (post_id, tag_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='게시글-태그 매핑';

-- 3.6 comment (댓글/대댓글)
CREATE TABLE comment (
    comment_id   BIGINT      NOT NULL AUTO_INCREMENT,
    post_id      BIGINT      NOT NULL                          COMMENT '→ post.post_id',
    member_id    BIGINT      NOT NULL                          COMMENT '작성자 → member.member_id',
    parent_id    BIGINT      NULL                              COMMENT '상위 댓글 → comment.comment_id',
    depth        INT         NOT NULL DEFAULT 0,
    content      TEXT        NOT NULL,
    is_anonymous BOOLEAN     NOT NULL DEFAULT FALSE,
    status       VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'         COMMENT 'ACTIVE / BLINDED / DELETED',
    like_count   INT         NOT NULL DEFAULT 0                COMMENT '반정규화',
    reply_count  INT         NOT NULL DEFAULT 0                COMMENT '반정규화',
    deleted_at   DATETIME    NULL,
    client_ip    VARCHAR(45) NULL                              COMMENT '작성자 IP (분쟁/법적 대응)',
    created_at   DATETIME    NOT NULL DEFAULT NOW(),
    updated_at   DATETIME    NOT NULL DEFAULT NOW() ON UPDATE NOW(),
    PRIMARY KEY (comment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='댓글/대댓글';

-- 3.7 post_like (게시글 좋아요)
CREATE TABLE post_like (
    post_like_id BIGINT   NOT NULL AUTO_INCREMENT,
    post_id      BIGINT   NOT NULL                             COMMENT '→ post.post_id',
    member_id    BIGINT   NOT NULL                             COMMENT '→ member.member_id',
    created_at   DATETIME NOT NULL DEFAULT NOW(),
    updated_at   DATETIME NOT NULL DEFAULT NOW() ON UPDATE NOW(),
    PRIMARY KEY (post_like_id),
    UNIQUE KEY uk_post_like (post_id, member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='게시글 좋아요';

-- 3.8 comment_like (댓글 좋아요)
CREATE TABLE comment_like (
    comment_like_id BIGINT   NOT NULL AUTO_INCREMENT,
    comment_id      BIGINT   NOT NULL                          COMMENT '→ comment.comment_id',
    member_id       BIGINT   NOT NULL                          COMMENT '→ member.member_id',
    created_at      DATETIME NOT NULL DEFAULT NOW(),
    updated_at      DATETIME NOT NULL DEFAULT NOW() ON UPDATE NOW(),
    PRIMARY KEY (comment_like_id),
    UNIQUE KEY uk_comment_like (comment_id, member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='댓글 좋아요';

-- 3.9 bookmark (북마크)
CREATE TABLE bookmark (
    bookmark_id BIGINT   NOT NULL AUTO_INCREMENT,
    member_id   BIGINT   NOT NULL                              COMMENT '→ member.member_id',
    post_id     BIGINT   NOT NULL                              COMMENT '→ post.post_id',
    created_at  DATETIME NOT NULL DEFAULT NOW(),
    updated_at  DATETIME NOT NULL DEFAULT NOW() ON UPDATE NOW(),
    PRIMARY KEY (bookmark_id),
    UNIQUE KEY uk_bookmark (member_id, post_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='북마크';


-- ------------------------------------------------------------
-- 4. 소셜 관계 도메인
-- ------------------------------------------------------------

-- 4.1 follow (팔로우)
CREATE TABLE follow (
    follow_id    BIGINT   NOT NULL AUTO_INCREMENT,
    follower_id  BIGINT   NOT NULL                             COMMENT '팔로우 하는 사람 → member.member_id',
    following_id BIGINT   NOT NULL                             COMMENT '팔로우 당하는 사람 → member.member_id',
    created_at   DATETIME NOT NULL DEFAULT NOW(),
    updated_at   DATETIME NOT NULL DEFAULT NOW() ON UPDATE NOW(),
    PRIMARY KEY (follow_id),
    UNIQUE KEY uk_follow (follower_id, following_id),
    CONSTRAINT chk_follow_self CHECK (follower_id <> following_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='팔로우';

-- 4.2 block (차단)
CREATE TABLE block (
    block_id   BIGINT   NOT NULL AUTO_INCREMENT,
    blocker_id BIGINT   NOT NULL                               COMMENT '차단한 사람 → member.member_id',
    blocked_id BIGINT   NOT NULL                               COMMENT '차단당한 사람 → member.member_id',
    created_at DATETIME NOT NULL DEFAULT NOW(),
    updated_at DATETIME NOT NULL DEFAULT NOW() ON UPDATE NOW(),
    PRIMARY KEY (block_id),
    UNIQUE KEY uk_block (blocker_id, blocked_id),
    CONSTRAINT chk_block_self CHECK (blocker_id <> blocked_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='차단';


-- ------------------------------------------------------------
-- 5. 그룹 도메인
-- ------------------------------------------------------------

-- 5.1 `group` (그룹) — board 상속 (board_id 보유)
-- MySQL 예약어이므로 백틱 사용
CREATE TABLE `group` (
    group_id        BIGINT       NOT NULL AUTO_INCREMENT,
    board_id        BIGINT       NOT NULL                      COMMENT '1:1 → board.board_id',
    name            VARCHAR(100) NOT NULL,
    description     TEXT         NULL,
    cover_image_url VARCHAR(500) NULL,
    category        VARCHAR(30)  NOT NULL                      COMMENT 'CLUB / SMALL_GROUP / PROJECT / STUDY',
    activity_type   VARCHAR(20)  NOT NULL DEFAULT 'ONLINE'     COMMENT 'ONLINE / OFFLINE / HYBRID',
    region          VARCHAR(50)  NULL                           COMMENT '활동 지역 (오프라인)',
    visibility      VARCHAR(20)  NOT NULL DEFAULT 'PUBLIC'     COMMENT 'PUBLIC / PARTIAL / PRIVATE',
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE'     COMMENT 'ACTIVE / CLOSED / FORCE_CLOSED',
    owner_id        BIGINT       NOT NULL                      COMMENT '그룹 소유자 → member.member_id',
    max_members     INT          NULL                          COMMENT 'NULL이면 무제한',
    member_count    INT          NOT NULL DEFAULT 1            COMMENT '반정규화',
    created_at      DATETIME     NOT NULL DEFAULT NOW(),
    updated_at      DATETIME     NOT NULL DEFAULT NOW() ON UPDATE NOW(),
    PRIMARY KEY (group_id),
    UNIQUE KEY uk_group_board (board_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='그룹';

-- 5.2 group_interest (그룹-관심태그 매핑)
CREATE TABLE group_interest (
    group_interest_id BIGINT   NOT NULL AUTO_INCREMENT,
    group_id          BIGINT   NOT NULL                        COMMENT '→ group.group_id',
    interest_id       BIGINT   NOT NULL                        COMMENT '→ interests.interest_id',
    created_at        DATETIME NOT NULL DEFAULT NOW(),
    updated_at        DATETIME NOT NULL DEFAULT NOW() ON UPDATE NOW(),
    PRIMARY KEY (group_interest_id),
    UNIQUE KEY uk_group_interest (group_id, interest_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='그룹-관심태그 매핑';

-- 5.3 group_member (그룹 멤버십)
CREATE TABLE group_member (
    group_member_id BIGINT      NOT NULL AUTO_INCREMENT,
    group_id        BIGINT      NOT NULL                       COMMENT '→ group.group_id',
    member_id       BIGINT      NOT NULL                       COMMENT '→ member.member_id',
    role            VARCHAR(20) NOT NULL DEFAULT 'MEMBER'      COMMENT 'OWNER / ADMIN / MEMBER',
    custom_title_id BIGINT      NULL                           COMMENT '커스텀 직책 → group_role.group_role_id',
    joined_at       DATETIME    NOT NULL,
    created_at      DATETIME    NOT NULL DEFAULT NOW(),
    updated_at      DATETIME    NOT NULL DEFAULT NOW() ON UPDATE NOW(),
    PRIMARY KEY (group_member_id),
    UNIQUE KEY uk_group_member (group_id, member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='그룹 멤버십';

-- 5.4 group_member_history (그룹 멤버 이력)
CREATE TABLE group_member_history (
    group_member_history_id BIGINT      NOT NULL AUTO_INCREMENT,
    group_id                BIGINT      NOT NULL               COMMENT '→ group.group_id',
    member_id               BIGINT      NOT NULL               COMMENT '대상 회원 → member.member_id',
    action                  VARCHAR(30) NOT NULL               COMMENT 'JOIN / LEAVE / KICKED / ROLE_CHANGE / TITLE_CHANGE',
    previous_role           VARCHAR(20) NULL,
    new_role                VARCHAR(20) NULL,
    reason                  TEXT        NULL,
    acted_by                BIGINT      NULL                   COMMENT '수행자 → member.member_id',
    source_system           VARCHAR(30) NULL                   COMMENT 'WEB_ADMIN / API / BATCH 등',
    client_ip               VARCHAR(45) NULL                   COMMENT '요청 IP',
    created_at              DATETIME    NOT NULL DEFAULT NOW(),
    updated_at              DATETIME    NOT NULL DEFAULT NOW() ON UPDATE NOW(),
    PRIMARY KEY (group_member_history_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='그룹 멤버 이력';

-- 5.5 group_role (그룹 커스텀 직책)
CREATE TABLE group_role (
    group_role_id BIGINT      NOT NULL AUTO_INCREMENT,
    group_id      BIGINT      NOT NULL                         COMMENT '→ group.group_id',
    title         VARCHAR(50) NOT NULL,
    display_order INT         NOT NULL DEFAULT 0,
    created_at    DATETIME    NOT NULL DEFAULT NOW(),
    updated_at    DATETIME    NOT NULL DEFAULT NOW() ON UPDATE NOW(),
    PRIMARY KEY (group_role_id),
    UNIQUE KEY uk_group_role_title (group_id, title)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='그룹 커스텀 직책';


-- ------------------------------------------------------------
-- 6. 모집/지원 도메인
-- ------------------------------------------------------------

-- 6.1 recruitment (모집글)
CREATE TABLE recruitment (
    recruitment_id      BIGINT       NOT NULL AUTO_INCREMENT,
    group_id            BIGINT       NOT NULL                  COMMENT '→ group.group_id',
    author_id           BIGINT       NOT NULL                  COMMENT '작성자 → member.member_id',
    title               VARCHAR(200) NOT NULL,
    description         TEXT         NOT NULL,
    positions           JSON         NULL                      COMMENT '모집 포지션 (예: [{"name":"프론트","count":2}])',
    requirements        TEXT         NULL,
    duration            VARCHAR(100) NULL                      COMMENT '활동 기간',
    goal                TEXT         NULL,
    process_description TEXT         NULL                      COMMENT '진행 방식',
    deadline            DATETIME     NULL                      COMMENT '모집 마감일',
    status              VARCHAR(20)  NOT NULL DEFAULT 'OPEN'   COMMENT 'OPEN / CLOSED / EXPIRED',
    application_count   INT          NOT NULL DEFAULT 0        COMMENT '반정규화',
    deleted_at          DATETIME     NULL,
    created_at          DATETIME     NOT NULL DEFAULT NOW(),
    updated_at          DATETIME     NOT NULL DEFAULT NOW() ON UPDATE NOW(),
    PRIMARY KEY (recruitment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='모집글';

-- 6.2 form_item (인앱 지원서 질문)
CREATE TABLE form_item (
    form_item_id   BIGINT       NOT NULL AUTO_INCREMENT,
    recruitment_id BIGINT       NOT NULL                       COMMENT '→ recruitment.recruitment_id',
    question       VARCHAR(500) NOT NULL,
    question_type  VARCHAR(20)  NOT NULL DEFAULT 'TEXT'        COMMENT 'TEXT / SELECT / MULTI_SELECT',
    is_required    BOOLEAN      NOT NULL DEFAULT TRUE,
    options        JSON         NULL                           COMMENT '선택형 옵션 (SELECT 타입)',
    display_order  TINYINT      NOT NULL                       COMMENT '질문 순서 (1~5)',
    created_at     DATETIME     NOT NULL DEFAULT NOW(),
    updated_at     DATETIME     NOT NULL DEFAULT NOW() ON UPDATE NOW(),
    PRIMARY KEY (form_item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='인앱 지원서 질문';

-- 6.3 recruitment_application (지원서)
CREATE TABLE recruitment_application (
    recruitment_application_id BIGINT       NOT NULL AUTO_INCREMENT,
    recruitment_id             BIGINT       NOT NULL            COMMENT '→ recruitment.recruitment_id',
    applicant_id               BIGINT       NOT NULL            COMMENT '지원자 → member.member_id',
    position                   VARCHAR(50)  NULL,
    portfolio_url              VARCHAR(500) NULL,
    status                     VARCHAR(20)  NOT NULL DEFAULT 'SUBMITTED' COMMENT 'SUBMITTED / IN_REVIEW / APPROVED / REJECTED / CANCELLED',
    reviewed_by                BIGINT       NULL                COMMENT '검토자 → member.member_id',
    reviewed_at                DATETIME     NULL,
    created_at                 DATETIME     NOT NULL DEFAULT NOW(),
    updated_at                 DATETIME     NOT NULL DEFAULT NOW() ON UPDATE NOW(),
    PRIMARY KEY (recruitment_application_id),
    UNIQUE KEY uk_recruitment_applicant (recruitment_id, applicant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='지원서';

-- 6.4 application_status_history (지원 상태 이력)
CREATE TABLE application_status_history (
    application_status_history_id BIGINT      NOT NULL AUTO_INCREMENT,
    recruitment_application_id    BIGINT      NOT NULL          COMMENT '→ recruitment_application.recruitment_application_id',
    previous_status               VARCHAR(20) NOT NULL,
    new_status                    VARCHAR(20) NOT NULL,
    changed_by                    BIGINT      NULL              COMMENT '변경자 → member.member_id',
    note                          TEXT        NULL              COMMENT '메모 (거절 사유 등)',
    source_system                 VARCHAR(30) NULL              COMMENT 'WEB_ADMIN / API / BATCH 등',
    client_ip                     VARCHAR(45) NULL              COMMENT '요청 IP',
    created_at                    DATETIME    NOT NULL DEFAULT NOW(),
    updated_at                    DATETIME    NOT NULL DEFAULT NOW() ON UPDATE NOW(),
    PRIMARY KEY (application_status_history_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='지원 상태 이력';

-- 6.5 form_item_answer (지원서 답변)
CREATE TABLE form_item_answer (
    form_item_answer_id        BIGINT   NOT NULL AUTO_INCREMENT,
    recruitment_application_id BIGINT   NOT NULL               COMMENT '→ recruitment_application.recruitment_application_id',
    form_item_id               BIGINT   NOT NULL               COMMENT '→ form_item.form_item_id',
    answer                     TEXT     NOT NULL,
    created_at                 DATETIME NOT NULL DEFAULT NOW(),
    updated_at                 DATETIME NOT NULL DEFAULT NOW() ON UPDATE NOW(),
    PRIMARY KEY (form_item_answer_id),
    UNIQUE KEY uk_app_form_answer (recruitment_application_id, form_item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='지원서 답변';

-- 6.6 application_chat_message (지원자 채팅 메시지)
CREATE TABLE application_chat_message (
    application_chat_message_id BIGINT   NOT NULL AUTO_INCREMENT,
    recruitment_application_id  BIGINT   NOT NULL              COMMENT '→ recruitment_application.recruitment_application_id',
    sender_id                   BIGINT   NOT NULL              COMMENT '발신자 → member.member_id',
    content                     TEXT     NOT NULL,
    is_read                     BOOLEAN  NOT NULL DEFAULT FALSE,
    deleted_at                  DATETIME NULL,
    client_ip                   VARCHAR(45) NULL               COMMENT '발신자 IP (분쟁/법적 대응)',
    created_at                  DATETIME NOT NULL DEFAULT NOW(),
    updated_at                  DATETIME NOT NULL DEFAULT NOW() ON UPDATE NOW(),
    PRIMARY KEY (application_chat_message_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='지원자 채팅 메시지';


-- ------------------------------------------------------------
-- 7. 알림 도메인
-- ------------------------------------------------------------

-- 7.1 member_notification (알림)
CREATE TABLE member_notification (
    member_notification_id BIGINT       NOT NULL AUTO_INCREMENT,
    member_id              BIGINT       NOT NULL               COMMENT '수신자 → member.member_id',
    type                   VARCHAR(30)  NOT NULL               COMMENT 'POST_COMMENT / COMMENT_REPLY / APPLICATION_RESULT 등',
    title                  VARCHAR(200) NOT NULL,
    content                VARCHAR(500) NULL,
    ref_type               VARCHAR(30)  NULL                   COMMENT '참조 대상 타입 (POST / COMMENT / GROUP / APPLICATION 등)',
    ref_id                 BIGINT       NULL                   COMMENT '참조 대상 ID',
    is_read                BOOLEAN      NOT NULL DEFAULT FALSE,
    read_at                DATETIME     NULL,
    created_at             DATETIME     NOT NULL DEFAULT NOW(),
    updated_at             DATETIME     NOT NULL DEFAULT NOW() ON UPDATE NOW(),
    PRIMARY KEY (member_notification_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='알림';

-- 7.2 notification_setting (알림 수신 설정) — Shared PK
CREATE TABLE notification_setting (
    member_id            BIGINT  NOT NULL                      COMMENT 'Shared PK → member.member_id',
    push_enabled         BOOLEAN NOT NULL DEFAULT TRUE,
    comment_on_post      BOOLEAN NOT NULL DEFAULT TRUE         COMMENT '내 글에 댓글',
    reply_on_comment     BOOLEAN NOT NULL DEFAULT TRUE         COMMENT '내 댓글에 답글',
    application_result   BOOLEAN NOT NULL DEFAULT TRUE,
    application_received BOOLEAN NOT NULL DEFAULT TRUE,
    group_notice         BOOLEAN NOT NULL DEFAULT TRUE,
    follow_activity      BOOLEAN NOT NULL DEFAULT FALSE,
    created_at           DATETIME NOT NULL DEFAULT NOW(),
    updated_at           DATETIME NOT NULL DEFAULT NOW() ON UPDATE NOW(),
    PRIMARY KEY (member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='알림 수신 설정';


-- ------------------------------------------------------------
-- 8. 신고/모더레이션 도메인
-- ------------------------------------------------------------

-- 8.1 report (신고)
CREATE TABLE report (
    report_id       BIGINT      NOT NULL AUTO_INCREMENT,
    reporter_id     BIGINT      NOT NULL                       COMMENT '신고자 → member.member_id',
    target_type     VARCHAR(30) NOT NULL                       COMMENT 'POST / COMMENT / MEMBER / GROUP / RECRUITMENT / CHAT_MESSAGE',
    target_id       BIGINT      NOT NULL,
    reason_category VARCHAR(50) NOT NULL                       COMMENT '신고 사유 카테고리 코드',
    reason_detail   TEXT        NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'OPEN'        COMMENT 'OPEN / TRIAGED / IN_REVIEW / ACTIONED / REJECTED / CLOSED',
    priority        VARCHAR(10) NULL                           COMMENT 'LOW / MEDIUM / HIGH / CRITICAL',
    assigned_to     BIGINT      NULL                           COMMENT '담당 운영자 → member.member_id',
    action_taken    VARCHAR(50) NULL                           COMMENT 'WARN / HIDE / BLIND / DELETE / SUSPEND / BAN 등',
    action_note     TEXT        NULL,
    actioned_at     DATETIME    NULL,
    closed_at       DATETIME    NULL                           COMMENT 'SLA 측정용',
    source_system   VARCHAR(30) NULL                           COMMENT 'WEB_ADMIN / API / BATCH 등',
    client_ip       VARCHAR(45) NULL                           COMMENT '요청 IP',
    created_at      DATETIME    NOT NULL DEFAULT NOW(),
    updated_at      DATETIME    NOT NULL DEFAULT NOW() ON UPDATE NOW(),
    PRIMARY KEY (report_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='신고';

-- 8.2 report_status_history (신고 처리 이력)
CREATE TABLE report_status_history (
    report_status_history_id BIGINT      NOT NULL AUTO_INCREMENT,
    report_id                BIGINT      NOT NULL              COMMENT '→ report.report_id',
    previous_status          VARCHAR(20) NOT NULL,
    new_status               VARCHAR(20) NOT NULL,
    changed_by               BIGINT      NULL                  COMMENT '변경자 → member.member_id',
    note                     TEXT        NULL,
    source_system            VARCHAR(30) NULL                  COMMENT 'WEB_ADMIN / API / BATCH 등',
    client_ip                VARCHAR(45) NULL                  COMMENT '요청 IP',
    created_at               DATETIME    NOT NULL DEFAULT NOW(),
    updated_at               DATETIME    NOT NULL DEFAULT NOW() ON UPDATE NOW(),
    PRIMARY KEY (report_status_history_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='신고 처리 이력';


-- ------------------------------------------------------------
-- 9. 감사/로그 도메인
-- ------------------------------------------------------------

-- 9.1 audit_log (공통 감사 로그)
CREATE TABLE audit_log (
    audit_log_id  BIGINT      NOT NULL AUTO_INCREMENT,
    actor_id      BIGINT      NULL                             COMMENT '수행자 → member.member_id (시스템이면 NULL)',
    actor_role    VARCHAR(20) NULL                             COMMENT 'ADMIN / MODERATOR / SYSTEM',
    action        VARCHAR(50) NOT NULL                         COMMENT 'USER_SUSPEND / CONTENT_DELETE / CONTENT_BLIND 등',
    target_type   VARCHAR(30) NOT NULL                         COMMENT 'MEMBER / POST / COMMENT / GROUP 등',
    target_id     BIGINT      NOT NULL,
    detail        JSON        NULL                             COMMENT '상세 정보 (이전값/변경값 등)',
    reason        TEXT        NULL,
    source_system VARCHAR(30) NULL                             COMMENT 'WEB_ADMIN / API / BATCH / SYSTEM',
    client_ip     VARCHAR(45) NULL,
    created_at    DATETIME    NOT NULL DEFAULT NOW(),
    updated_at    DATETIME    NOT NULL DEFAULT NOW() ON UPDATE NOW(),
    PRIMARY KEY (audit_log_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='공통 감사 로그 (Insert Only)';


-- ------------------------------------------------------------
-- 10. 기타
-- ------------------------------------------------------------

-- 10.1 calendar_item (캘린더 항목 — Post-MVP)
CREATE TABLE calendar_item (
    calendar_item_id BIGINT       NOT NULL AUTO_INCREMENT,
    group_id         BIGINT       NOT NULL                     COMMENT '→ group.group_id',
    member_id        BIGINT       NOT NULL                     COMMENT '작성자 → member.member_id',
    title            VARCHAR(200) NOT NULL,
    description      TEXT         NULL,
    start_at         DATETIME     NOT NULL,
    end_at           DATETIME     NULL,
    is_all_day       BOOLEAN      NOT NULL DEFAULT FALSE,
    location         VARCHAR(200) NULL,
    deleted_at       DATETIME     NULL,
    created_at       DATETIME     NOT NULL DEFAULT NOW(),
    updated_at       DATETIME     NOT NULL DEFAULT NOW() ON UPDATE NOW(),
    PRIMARY KEY (calendar_item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='캘린더 항목 (Post-MVP)';
