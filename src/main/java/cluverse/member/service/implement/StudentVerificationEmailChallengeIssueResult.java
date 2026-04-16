package cluverse.member.service.implement;

import cluverse.member.domain.StudentVerificationEmailChallenge;

public record StudentVerificationEmailChallengeIssueResult(
        String code,
        StudentVerificationEmailChallenge challenge
) {
}
