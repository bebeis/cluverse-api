CREATE TABLE student_verification (
    student_verification_id BIGINT NOT NULL AUTO_INCREMENT,
    member_id BIGINT NOT NULL,
    university_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'NONE' COMMENT 'NONE / PENDING / APPROVED / REJECTED',
    method VARCHAR(30) NULL COMMENT 'SCHOOL_EMAIL / STUDENT_ID_CARD / ENROLLMENT_CERT',
    school_email VARCHAR(255) NULL COMMENT '인증에 사용한 학교 이메일',
    rejected_reason VARCHAR(50) NULL COMMENT '인증 거절 사유 코드',
    requested_at DATETIME NULL COMMENT '최근 인증 요청 시각',
    verified_at DATETIME NULL COMMENT '인증 완료 시각',
    created_at DATETIME NOT NULL DEFAULT NOW(),
    updated_at DATETIME NOT NULL DEFAULT NOW() ON UPDATE NOW(),
    PRIMARY KEY (student_verification_id),
    UNIQUE KEY uk_student_verification_member_id (member_id),
    KEY idx_student_verification_university_id (university_id),
    KEY idx_student_verification_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='학생 인증 현재 상태';

CREATE TABLE student_verification_email_challenge (
    student_verification_email_challenge_id BIGINT NOT NULL AUTO_INCREMENT,
    student_verification_id BIGINT NOT NULL,
    challenge_id VARCHAR(64) NOT NULL COMMENT '외부에 노출하는 인증 시도 식별자',
    email VARCHAR(255) NOT NULL COMMENT '인증 코드를 발송한 학교 이메일',
    code_hash VARCHAR(255) NOT NULL COMMENT '인증 코드 해시',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING / VERIFIED / EXPIRED',
    expires_at DATETIME NOT NULL COMMENT '인증 코드 만료 시각',
    attempt_count INT NOT NULL DEFAULT 0 COMMENT '코드 확인 시도 횟수',
    verified_at DATETIME NULL COMMENT '코드 확인 완료 시각',
    created_at DATETIME NOT NULL DEFAULT NOW(),
    updated_at DATETIME NOT NULL DEFAULT NOW() ON UPDATE NOW(),
    PRIMARY KEY (student_verification_email_challenge_id),
    UNIQUE KEY uk_student_verification_email_challenge_id (challenge_id),
    KEY idx_student_verification_email_challenge_verification_id (student_verification_id),
    KEY idx_student_verification_email_challenge_email (email),
    KEY idx_student_verification_email_challenge_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='학생 학교 이메일 인증 시도';

-- 기존 member/member_credential 기반 학생 인증 데이터를 신규 현재 상태 테이블로 이관한다.
-- 회원별로 가장 최신 인증 자료 1건을 선택하고, 인증 자료가 없으면 member의 기존 인증 상태를 사용한다.
INSERT INTO student_verification (
    member_id,
    university_id,
    status,
    method,
    school_email,
    rejected_reason,
    requested_at,
    verified_at,
    created_at,
    updated_at
)
SELECT
    m.member_id,
    m.university_id,
    COALESCE(latest_credential.status, m.verification_status, 'NONE') AS status,
    latest_credential.credential_type AS method,
    CASE
        WHEN latest_credential.credential_type = 'SCHOOL_EMAIL'
            THEN LOWER(TRIM(latest_credential.credential_value))
        ELSE NULL
    END AS school_email,
    COALESCE(latest_credential.rejected_reason, m.verification_rejected_reason) AS rejected_reason,
    latest_credential.created_at AS requested_at,
    CASE
        WHEN COALESCE(latest_credential.status, m.verification_status) = 'APPROVED'
            THEN COALESCE(latest_credential.reviewed_at, latest_credential.updated_at)
        ELSE NULL
    END AS verified_at,
    COALESCE(latest_credential.created_at, m.created_at) AS created_at,
    COALESCE(latest_credential.updated_at, m.updated_at) AS updated_at
FROM member m
LEFT JOIN (
    SELECT ranked_credential.*
    FROM (
        SELECT
            mc.*,
            ROW_NUMBER() OVER (
                PARTITION BY mc.member_id
                ORDER BY
                    COALESCE(mc.reviewed_at, mc.updated_at, mc.created_at) DESC,
                    mc.member_credential_id DESC
            ) AS rn
        FROM member_credential mc
    ) ranked_credential
    WHERE ranked_credential.rn = 1
) latest_credential
    ON latest_credential.member_id = m.member_id
WHERE m.university_id IS NOT NULL
  AND (
      m.verification_status <> 'NONE'
      OR latest_credential.member_credential_id IS NOT NULL
  )
ON DUPLICATE KEY UPDATE
    university_id = VALUES(university_id),
    status = VALUES(status),
    method = VALUES(method),
    school_email = VALUES(school_email),
    rejected_reason = VALUES(rejected_reason),
    requested_at = VALUES(requested_at),
    verified_at = VALUES(verified_at),
    updated_at = VALUES(updated_at);

-- 검증 후 member_credential은 제거 가능하다.
-- 현재 애플리케이션 코드가 member.verification_status를 아직 권한 판단 캐시로 사용하므로
-- member.verification_status / member.verification_rejected_reason 제거는 읽기 경로 전환 이후 별도 migration에서 처리한다.
-- DROP TABLE member_credential;
