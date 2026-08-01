CREATE INDEX idx_post_popularity_scan
    ON post (status, created_at, post_id);

CREATE TABLE popular_post (
    popular_post_id           BIGINT       NOT NULL AUTO_INCREMENT,
    algorithm_version         VARCHAR(10)  NOT NULL,
    post_id                   BIGINT       NOT NULL,
    board_id                  BIGINT       NOT NULL,
    promoted_at               DATETIME     NOT NULL,
    finalize_at               DATETIME     NOT NULL,
    score_at_promotion        BIGINT       NOT NULL,
    promotion_trigger         VARCHAR(30)  NOT NULL,
    promotion_score_threshold BIGINT      NOT NULL,
    like_gate_threshold       INT          NOT NULL,
    comment_gate_threshold    INT          NOT NULL,
    score                     BIGINT       NULL,
    like_count                BIGINT       NULL,
    comment_count             BIGINT       NULL,
    view_count                BIGINT       NULL,
    finalized_at              DATETIME     NULL,
    created_at                DATETIME     NOT NULL DEFAULT NOW(),
    updated_at                DATETIME     NOT NULL DEFAULT NOW() ON UPDATE NOW(),
    PRIMARY KEY (popular_post_id),
    UNIQUE KEY uk_popular_post_version_post (algorithm_version, post_id),
    INDEX idx_popular_recent (algorithm_version, finalized_at, promoted_at DESC, post_id DESC),
    INDEX idx_popular_ranking (algorithm_version, finalized_at, score DESC, post_id DESC),
    INDEX idx_popular_finalize_due (finalized_at, finalize_at, post_id),
    INDEX idx_popular_finalize_post (post_id, finalized_at, finalize_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE board_popularity_policy (
    board_id         BIGINT       NOT NULL,
    promotion_score  BIGINT       NOT NULL,
    like_gate        INT          NOT NULL,
    comment_gate     INT          NOT NULL,
    sample_size      INT          NOT NULL,
    policy_source    VARCHAR(20)  NOT NULL,
    computed_at      DATETIME     NOT NULL,
    created_at       DATETIME     NOT NULL DEFAULT NOW(),
    updated_at       DATETIME     NOT NULL DEFAULT NOW() ON UPDATE NOW(),
    PRIMARY KEY (board_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE popularity_candidate (
    post_id          BIGINT       NOT NULL,
    board_id         BIGINT       NOT NULL,
    registered_at    DATETIME     NOT NULL,
    next_check_at    DATETIME     NOT NULL,
    expires_at       DATETIME     NOT NULL,
    last_checked_at  DATETIME     NULL,
    created_at       DATETIME     NOT NULL DEFAULT NOW(),
    updated_at       DATETIME     NOT NULL DEFAULT NOW() ON UPDATE NOW(),
    PRIMARY KEY (post_id),
    INDEX idx_candidate_check (next_check_at, post_id),
    INDEX idx_candidate_expire (expires_at, post_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE popularity_finalization_claim (
    post_id       BIGINT       NOT NULL,
    claim_token   VARCHAR(36)  NOT NULL,
    claimed_at    DATETIME     NOT NULL,
    created_at    DATETIME     NOT NULL DEFAULT NOW(),
    updated_at    DATETIME     NOT NULL DEFAULT NOW() ON UPDATE NOW(),
    PRIMARY KEY (post_id),
    INDEX idx_finalization_claimed_at (claimed_at, post_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
