package cluverse.member.service.implement;

import cluverse.common.exception.NotFoundException;
import cluverse.member.domain.StudentVerification;
import cluverse.member.domain.StudentVerificationEmailChallenge;
import cluverse.member.domain.StudentVerificationEmailChallengeStatus;
import cluverse.member.exception.MemberExceptionMessage;
import cluverse.member.repository.StudentVerificationEmailChallengeRepository;
import cluverse.member.repository.StudentVerificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentVerificationReader {

    private final StudentVerificationRepository studentVerificationRepository;
    private final StudentVerificationEmailChallengeRepository emailChallengeRepository;

    public Optional<StudentVerification> findByMemberId(Long memberId) {
        return studentVerificationRepository.findByMemberId(memberId);
    }

    public StudentVerification findOrCreateByMemberId(Long memberId, Long universityId) {
        return findByMemberId(memberId)
                .orElseGet(() -> StudentVerification.create(memberId, universityId));
    }

    public StudentVerification readOrThrow(Long studentVerificationId) {
        return studentVerificationRepository.findById(studentVerificationId)
                .orElseThrow(() -> new NotFoundException(
                        MemberExceptionMessage.STUDENT_VERIFICATION_NOT_FOUND.getMessage()
                ));
    }

    public StudentVerificationEmailChallenge readEmailChallengeOrThrow(String challengeId) {
        return emailChallengeRepository.findByChallengeId(challengeId)
                .orElseThrow(() -> new NotFoundException(
                        MemberExceptionMessage.STUDENT_VERIFICATION_EMAIL_CHALLENGE_NOT_FOUND.getMessage()
                ));
    }

    public Optional<StudentVerificationEmailChallenge> findLatestPendingEmailChallenge(Long studentVerificationId) {
        return emailChallengeRepository.findFirstByStudentVerificationIdAndStatusOrderByCreatedAtDesc(
                studentVerificationId,
                StudentVerificationEmailChallengeStatus.PENDING
        );
    }

    public boolean isLatestPendingEmailChallenge(StudentVerificationEmailChallenge challenge) {
        return findLatestPendingEmailChallenge(challenge.getStudentVerificationId())
                .map(latestChallenge -> latestChallenge.getId().equals(challenge.getId()))
                .orElse(false);
    }
}
