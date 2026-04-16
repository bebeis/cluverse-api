package cluverse.member.service.implement;

import cluverse.common.config.PasswordConfig;
import cluverse.common.exception.BadRequestException;
import cluverse.common.exception.NotFoundException;
import cluverse.member.domain.Member;
import cluverse.member.domain.StudentVerification;
import cluverse.member.domain.StudentVerificationEmailChallenge;
import cluverse.member.exception.MemberExceptionMessage;
import cluverse.university.domain.University;
import cluverse.university.service.implement.UniversityReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

import static cluverse.common.util.StringNormalizer.extractEmailDomain;
import static cluverse.common.util.StringNormalizer.normalizeOptionalDomain;
import static cluverse.common.util.StringNormalizer.requireNormalizedEmail;

@Component
@RequiredArgsConstructor
public class StudentVerificationContextReader {

    private final MemberReader memberReader;
    private final UniversityReader universityReader;
    private final StudentVerificationReader studentVerificationReader;
    private final PasswordConfig passwordConfig;

    public StudentVerificationEmailChallengeRequestContext readEmailChallengeRequestContext(
            Long memberId,
            String requestEmail
    ) {
        Member member = memberReader.readOrThrow(memberId);
        validateNotVerified(member);
        validateUniversityRegistered(member);

        University university = universityReader.readOrThrow(member.getUniversityId());
        String email = requireNormalizedEmail(requestEmail);
        validateSchoolEmailDomain(email, university);

        StudentVerification studentVerification =
                studentVerificationReader.findOrCreateByMemberId(memberId, university.getId());
        validateNotVerified(studentVerification);

        return new StudentVerificationEmailChallengeRequestContext(
                member,
                university,
                email,
                studentVerification
        );
    }

    public StudentVerificationEmailConfirmationContext readEmailConfirmationContext(
            Long memberId,
            String challengeId,
            String code,
            LocalDateTime now
    ) {
        Member member = memberReader.readOrThrow(memberId);
        StudentVerificationEmailChallenge challenge =
                studentVerificationReader.readEmailChallengeOrThrow(challengeId);
        StudentVerification studentVerification =
                studentVerificationReader.readOrThrow(challenge.getStudentVerificationId());

        validateChallengeOwner(studentVerification, memberId);
        validateLatestPendingChallenge(challenge);
        validateChallengeNotExpired(challenge, now);

        challenge.increaseAttemptCount();
        validateCode(code, challenge.getCodeHash());

        University university = universityReader.readOrThrow(studentVerification.getUniversityId());
        return new StudentVerificationEmailConfirmationContext(
                member,
                university,
                studentVerification,
                challenge
        );
    }

    private void validateNotVerified(Member member) {
        if (member.isVerified()) {
            throw new BadRequestException(
                    MemberExceptionMessage.STUDENT_VERIFICATION_ALREADY_APPROVED.getMessage()
            );
        }
    }

    private void validateNotVerified(StudentVerification studentVerification) {
        if (studentVerification.isVerified()) {
            throw new BadRequestException(
                    MemberExceptionMessage.STUDENT_VERIFICATION_ALREADY_APPROVED.getMessage()
            );
        }
    }

    private void validateUniversityRegistered(Member member) {
        if (!member.hasUniversity()) {
            throw new BadRequestException(
                    MemberExceptionMessage.UNIVERSITY_REGISTRATION_REQUIRED.getMessage()
            );
        }
    }

    private void validateSchoolEmailDomain(String email, University university) {
        String emailDomain = extractEmailDomain(email);
        String universityEmailDomain = normalizeOptionalDomain(university.getEmailDomain());
        if (universityEmailDomain == null) {
            throw new BadRequestException(
                    MemberExceptionMessage.STUDENT_VERIFICATION_EMAIL_DOMAIN_REQUIRED.getMessage()
            );
        }

        if (!emailDomain.equals(universityEmailDomain)) {
            throw new BadRequestException(
                    MemberExceptionMessage.STUDENT_VERIFICATION_EMAIL_DOMAIN_MISMATCH.getMessage()
            );
        }
    }

    private void validateChallengeOwner(StudentVerification studentVerification, Long memberId) {
        if (!studentVerification.isOwnedBy(memberId)) {
            throw new NotFoundException(
                    MemberExceptionMessage.STUDENT_VERIFICATION_EMAIL_CHALLENGE_NOT_FOUND.getMessage()
            );
        }
    }

    private void validateLatestPendingChallenge(StudentVerificationEmailChallenge challenge) {
        if (challenge.isReplaced()) {
            throw new BadRequestException(
                    MemberExceptionMessage.STUDENT_VERIFICATION_EMAIL_CHALLENGE_REPLACED.getMessage()
            );
        }

        if (challenge.isExpiredStatus()) {
            throw new BadRequestException(
                    MemberExceptionMessage.STUDENT_VERIFICATION_EMAIL_CHALLENGE_EXPIRED.getMessage()
            );
        }

        if (!challenge.isPending()) {
            throw new BadRequestException(
                    MemberExceptionMessage.STUDENT_VERIFICATION_EMAIL_CHALLENGE_INVALID_STATUS.getMessage()
            );
        }

        if (!studentVerificationReader.isLatestPendingEmailChallenge(challenge)) {
            challenge.replace();
            throw new BadRequestException(
                    MemberExceptionMessage.STUDENT_VERIFICATION_EMAIL_CHALLENGE_REPLACED.getMessage()
            );
        }
    }

    private void validateChallengeNotExpired(StudentVerificationEmailChallenge challenge, LocalDateTime now) {
        if (challenge.isExpired(now)) {
            challenge.expire();
            throw new BadRequestException(
                    MemberExceptionMessage.STUDENT_VERIFICATION_EMAIL_CHALLENGE_EXPIRED.getMessage()
            );
        }
    }

    private void validateCode(String code, String codeHash) {
        if (!passwordConfig.matches(code, codeHash)) {
            throw new BadRequestException(
                    MemberExceptionMessage.STUDENT_VERIFICATION_EMAIL_CODE_INVALID.getMessage()
            );
        }
    }
}
