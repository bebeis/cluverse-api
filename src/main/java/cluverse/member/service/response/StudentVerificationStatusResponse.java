package cluverse.member.service.response;

import cluverse.member.domain.StudentVerificationMethod;
import cluverse.member.domain.VerificationStatus;

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
}
