package cluverse.member.service.implement;

import cluverse.common.config.PasswordConfig;
import cluverse.member.domain.StudentVerification;
import cluverse.member.domain.StudentVerificationEmailChallenge;
import cluverse.member.domain.StudentVerificationEmailChallengeStatus;
import cluverse.member.repository.StudentVerificationEmailChallengeRepository;
import cluverse.member.repository.StudentVerificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Transactional
public class StudentVerificationWriter {

    private final StudentVerificationRepository studentVerificationRepository;
    private final StudentVerificationEmailChallengeRepository emailChallengeRepository;
    private final StudentVerificationCodeGenerator codeGenerator;
    private final PasswordConfig passwordConfig;

    public StudentVerification save(StudentVerification studentVerification) {
        return studentVerificationRepository.save(studentVerification);
    }

    public StudentVerificationEmailChallenge saveEmailChallenge(
            StudentVerificationEmailChallenge emailChallenge
    ) {
        return emailChallengeRepository.save(emailChallenge);
    }

    public StudentVerification requestSchoolEmailVerification(
            StudentVerification studentVerification,
            Long universityId,
            String email,
            LocalDateTime requestedAt
    ) {
        studentVerification.requestSchoolEmailVerification(universityId, email, requestedAt);
        StudentVerification savedVerification = save(studentVerification);
        replacePendingEmailChallenges(savedVerification.getId());
        return savedVerification;
    }

    public StudentVerificationEmailChallengeIssueResult issueEmailChallenge(
            Long studentVerificationId,
            String email,
            LocalDateTime expiresAt
    ) {
        String code = codeGenerator.generateCode();
        StudentVerificationEmailChallenge challenge = StudentVerificationEmailChallenge.create(
                studentVerificationId,
                codeGenerator.generateChallengeId(),
                email,
                passwordConfig.encode(code),
                expiresAt
        );
        return new StudentVerificationEmailChallengeIssueResult(code, saveEmailChallenge(challenge));
    }

    public void replacePendingEmailChallenges(Long studentVerificationId) {
        emailChallengeRepository.replacePendingChallenges(
                studentVerificationId,
                StudentVerificationEmailChallengeStatus.PENDING,
                StudentVerificationEmailChallengeStatus.REPLACED
        );
    }

    public int expirePendingEmailChallenges(LocalDateTime now) {
        return emailChallengeRepository.expirePendingChallenges(
                StudentVerificationEmailChallengeStatus.PENDING,
                StudentVerificationEmailChallengeStatus.EXPIRED,
                now
        );
    }
}
