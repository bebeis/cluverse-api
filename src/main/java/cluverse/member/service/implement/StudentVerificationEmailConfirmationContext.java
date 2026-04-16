package cluverse.member.service.implement;

import cluverse.member.domain.Member;
import cluverse.member.domain.StudentVerification;
import cluverse.member.domain.StudentVerificationEmailChallenge;
import cluverse.university.domain.University;

public record StudentVerificationEmailConfirmationContext(
        Member member,
        University university,
        StudentVerification studentVerification,
        StudentVerificationEmailChallenge challenge
) {
}
