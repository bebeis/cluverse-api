package cluverse.member.domain;

import cluverse.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;

import static cluverse.common.util.StringNormalizer.requireTrimmed;
import static cluverse.common.util.StringNormalizer.requireNormalizedEmail;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudentVerificationEmailChallenge extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "student_verification_email_challenge_id")
    private Long id;

    @Column(nullable = false)
    private Long studentVerificationId;

    @Column(nullable = false, unique = true)
    private String challengeId;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String codeHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StudentVerificationEmailChallengeStatus status = StudentVerificationEmailChallengeStatus.PENDING;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private int attemptCount;

    private LocalDateTime verifiedAt;

    private StudentVerificationEmailChallenge(
            Long studentVerificationId,
            String challengeId,
            String email,
            String codeHash,
            LocalDateTime expiresAt
    ) {
        this.studentVerificationId = Objects.requireNonNull(studentVerificationId);
        this.challengeId = requireTrimmed(challengeId);
        this.email = requireNormalizedEmail(email);
        this.codeHash = requireTrimmed(codeHash);
        this.expiresAt = Objects.requireNonNull(expiresAt);
    }

    public static StudentVerificationEmailChallenge create(
            Long studentVerificationId,
            String challengeId,
            String email,
            String codeHash,
            LocalDateTime expiresAt
    ) {
        return new StudentVerificationEmailChallenge(
                studentVerificationId,
                challengeId,
                email,
                codeHash,
                expiresAt
        );
    }

    public void increaseAttemptCount() {
        this.attemptCount++;
    }

    public void verify(LocalDateTime verifiedAt) {
        this.status = StudentVerificationEmailChallengeStatus.VERIFIED;
        this.verifiedAt = Objects.requireNonNull(verifiedAt);
    }

    public void expire() {
        this.status = StudentVerificationEmailChallengeStatus.EXPIRED;
    }

    public void replace() {
        this.status = StudentVerificationEmailChallengeStatus.REPLACED;
    }

    public boolean isExpired(LocalDateTime now) {
        return !expiresAt.isAfter(now);
    }

    public boolean isPending() {
        return this.status == StudentVerificationEmailChallengeStatus.PENDING;
    }

    public boolean isReplaced() {
        return this.status == StudentVerificationEmailChallengeStatus.REPLACED;
    }

    public boolean isExpiredStatus() {
        return this.status == StudentVerificationEmailChallengeStatus.EXPIRED;
    }

    public boolean isVerified() {
        return this.status == StudentVerificationEmailChallengeStatus.VERIFIED;
    }
}
