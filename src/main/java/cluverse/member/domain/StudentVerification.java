package cluverse.member.domain;

import cluverse.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import static cluverse.common.util.StringNormalizer.requireTrimmed;
import static cluverse.common.util.StringNormalizer.requireNormalizedEmail;
import static java.util.Objects.requireNonNull;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudentVerification extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "student_verification_id")
    private Long id;

    @Column(nullable = false, unique = true)
    private Long memberId;

    @Column(nullable = false)
    private Long universityId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VerificationStatus status = VerificationStatus.NONE;

    @Enumerated(EnumType.STRING)
    private StudentVerificationMethod method;

    private String schoolEmail;

    private String rejectedReason;

    private LocalDateTime requestedAt;

    private LocalDateTime verifiedAt;

    private StudentVerification(Long memberId, Long universityId) {
        this.memberId = requireNonNull(memberId);
        this.universityId = requireNonNull(universityId);
    }

    public static StudentVerification create(Long memberId, Long universityId) {
        return new StudentVerification(memberId, universityId);
    }

    public void requestSchoolEmailVerification(Long universityId, String schoolEmail, LocalDateTime requestedAt) {
        this.universityId = requireNonNull(universityId);
        this.status = VerificationStatus.PENDING;
        this.method = StudentVerificationMethod.SCHOOL_EMAIL;
        this.schoolEmail = requireNormalizedEmail(schoolEmail);
        this.rejectedReason = null;
        this.requestedAt = requireNonNull(requestedAt);
        this.verifiedAt = null;
    }

    public void approve(LocalDateTime verifiedAt) {
        this.status = VerificationStatus.APPROVED;
        this.rejectedReason = null;
        this.verifiedAt = requireNonNull(verifiedAt);
    }

    public void reject(String rejectedReason) {
        this.status = VerificationStatus.REJECTED;
        this.rejectedReason = requireTrimmed(rejectedReason);
        this.verifiedAt = null;
    }

    public boolean isVerified() {
        return this.status == VerificationStatus.APPROVED;
    }

    public boolean isOwnedBy(Long memberId) {
        return this.memberId.equals(memberId);
    }
}
