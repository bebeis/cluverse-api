package cluverse.member.service.implement;

import cluverse.member.domain.Member;
import cluverse.member.domain.StudentVerification;
import cluverse.university.domain.University;

public record StudentVerificationEmailChallengeRequestContext(
        Member member,
        University university,
        String email,
        StudentVerification studentVerification
) {
}
