package cluverse.member.service;

import cluverse.member.service.request.StudentVerificationEmailChallengeCreateRequest;
import cluverse.member.service.request.StudentVerificationEmailConfirmationCreateRequest;
import cluverse.member.service.response.StudentVerificationEmailChallengeResponse;
import cluverse.member.service.response.StudentVerificationStatusResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class StudentVerificationService {

    public StudentVerificationEmailChallengeResponse createEmailChallenge(
            Long memberId,
            StudentVerificationEmailChallengeCreateRequest request
    ) {
        throw new UnsupportedOperationException("학교 이메일 인증 challenge 생성은 아직 구현되지 않았습니다.");
    }

    public StudentVerificationStatusResponse createEmailConfirmation(
            Long memberId,
            String challengeId,
            StudentVerificationEmailConfirmationCreateRequest request
    ) {
        throw new UnsupportedOperationException("학교 이메일 인증 confirmation 생성은 아직 구현되지 않았습니다.");
    }
}
