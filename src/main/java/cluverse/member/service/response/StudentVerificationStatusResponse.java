package cluverse.member.service.response;

import cluverse.member.domain.Member;
import cluverse.member.domain.StudentVerification;
import cluverse.member.domain.StudentVerificationMethod;
import cluverse.member.domain.VerificationStatus;
import cluverse.university.domain.University;

import java.time.LocalDateTime;

public record StudentVerificationStatusResponse(
        VerificationStatus verificationStatus,
        boolean verified,
        StudentVerificationMethod verificationMethod,
        StudentVerificationUniversityResponse university,
        String schoolEmail,
        String rejectedReason,
        LocalDateTime requestedAt,
        LocalDateTime verifiedAt
) {

    public static StudentVerificationStatusResponse from(
            StudentVerification studentVerification,
            University university
    ) {
        return new StudentVerificationStatusResponse(
                studentVerification.getStatus(),
                studentVerification.isVerified(),
                studentVerification.getMethod(),
                StudentVerificationUniversityResponse.from(university),
                studentVerification.getSchoolEmail(),
                studentVerification.getRejectedReason(),
                studentVerification.getRequestedAt(),
                studentVerification.getVerifiedAt()
        );
    }

    public static StudentVerificationStatusResponse fromCompatibility(Member member, University university) {
        return new StudentVerificationStatusResponse(
                member.getVerificationStatus(),
                member.isVerified(),
                null,
                StudentVerificationUniversityResponse.from(university),
                null,
                member.getVerificationRejectedReason(),
                null,
                null
        );
    }
}
