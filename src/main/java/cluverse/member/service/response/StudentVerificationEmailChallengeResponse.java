package cluverse.member.service.response;

import cluverse.member.domain.StudentVerification;
import cluverse.member.domain.StudentVerificationEmailChallenge;
import cluverse.member.domain.VerificationStatus;

import java.time.LocalDateTime;

public record StudentVerificationEmailChallengeResponse(
        String challengeId,
        VerificationStatus verificationStatus,
        String email,
        LocalDateTime expiresAt,
        long retryAfterSeconds
) {

    public static StudentVerificationEmailChallengeResponse of(
            StudentVerification studentVerification,
            StudentVerificationEmailChallenge challenge,
            long retryAfterSeconds
    ) {
        return new StudentVerificationEmailChallengeResponse(
                challenge.getChallengeId(),
                studentVerification.getStatus(),
                challenge.getEmail(),
                challenge.getExpiresAt(),
                retryAfterSeconds
        );
    }
}
