package cluverse.member.service.response;

import cluverse.member.domain.VerificationStatus;

import java.time.LocalDateTime;

public record StudentVerificationEmailChallengeResponse(
        String challengeId,
        VerificationStatus verificationStatus,
        String email,
        LocalDateTime expiresAt,
        long retryAfterSeconds
) {
}
