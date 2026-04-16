package cluverse.member.service;

import cluverse.member.client.StudentVerificationEmailClient;
import cluverse.member.domain.StudentVerification;
import cluverse.member.service.implement.StudentVerificationContextReader;
import cluverse.member.service.implement.StudentVerificationEmailChallengeIssueResult;
import cluverse.member.service.implement.StudentVerificationEmailChallengeRequestContext;
import cluverse.member.service.implement.StudentVerificationEmailConfirmationContext;
import cluverse.member.service.implement.StudentVerificationWriter;
import cluverse.member.service.request.StudentVerificationEmailChallengeCreateRequest;
import cluverse.member.service.request.StudentVerificationEmailConfirmationCreateRequest;
import cluverse.member.service.response.StudentVerificationEmailChallengeResponse;
import cluverse.member.service.response.StudentVerificationStatusResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class StudentVerificationService {

    private static final long EMAIL_CHALLENGE_RETRY_AFTER_SECONDS = 60L;
    private static final long EMAIL_CHALLENGE_EXPIRE_MINUTES = 5L;

    private final StudentVerificationContextReader studentVerificationContextReader;
    private final StudentVerificationWriter studentVerificationWriter;
    private final StudentVerificationEmailClient emailClient;

    public StudentVerificationEmailChallengeResponse createEmailChallenge(
            Long memberId,
            StudentVerificationEmailChallengeCreateRequest request
    ) {
        StudentVerificationEmailChallengeRequestContext context =
                studentVerificationContextReader.readEmailChallengeRequestContext(memberId, request.email());
        LocalDateTime now = LocalDateTime.now();

        StudentVerification studentVerification = studentVerificationWriter.requestSchoolEmailVerification(
                context.studentVerification(),
                context.university().getId(),
                context.email(),
                now
        );
        StudentVerificationEmailChallengeIssueResult issueResult = studentVerificationWriter.issueEmailChallenge(
                studentVerification.getId(),
                context.email(),
                now.plusMinutes(EMAIL_CHALLENGE_EXPIRE_MINUTES)
        );

        context.member().requestVerification();
        emailClient.sendVerificationCode(
                context.email(),
                issueResult.code(),
                issueResult.challenge().getExpiresAt()
        );

        return StudentVerificationEmailChallengeResponse.of(
                studentVerification,
                issueResult.challenge(),
                EMAIL_CHALLENGE_RETRY_AFTER_SECONDS
        );
    }

    public StudentVerificationStatusResponse createEmailConfirmation(
            Long memberId,
            String challengeId,
            StudentVerificationEmailConfirmationCreateRequest request
    ) {
        LocalDateTime now = LocalDateTime.now();
        StudentVerificationEmailConfirmationContext context =
                studentVerificationContextReader.readEmailConfirmationContext(
                        memberId,
                        challengeId,
                        request.code(),
                        now
                );

        context.challenge().verify(now);
        context.studentVerification().approve(now);
        context.member().approveVerification();

        return StudentVerificationStatusResponse.from(
                context.studentVerification(),
                context.university()
        );
    }
}
