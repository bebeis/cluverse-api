package cluverse.member.service.implement;

import cluverse.common.config.PasswordConfig;
import cluverse.member.domain.StudentVerification;
import cluverse.member.domain.StudentVerificationEmailChallenge;
import cluverse.member.domain.StudentVerificationEmailChallengeStatus;
import cluverse.member.domain.VerificationStatus;
import cluverse.member.repository.StudentVerificationEmailChallengeRepository;
import cluverse.member.repository.StudentVerificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentVerificationWriterTest {

    @Mock
    private StudentVerificationRepository studentVerificationRepository;

    @Mock
    private StudentVerificationEmailChallengeRepository emailChallengeRepository;

    @Mock
    private StudentVerificationCodeGenerator codeGenerator;

    @Mock
    private PasswordConfig passwordConfig;

    @InjectMocks
    private StudentVerificationWriter studentVerificationWriter;

    @Test
    void 학교_이메일_인증_요청시_인증_상태를_저장하고_기존_pending_challenge를_교체한다() {
        // given
        StudentVerification studentVerification = StudentVerification.create(1L, 10L);
        ReflectionTestUtils.setField(studentVerification, "id", 100L);
        LocalDateTime requestedAt = LocalDateTime.of(2026, 4, 16, 14, 55);

        when(studentVerificationRepository.save(studentVerification)).thenReturn(studentVerification);

        // when
        StudentVerification result = studentVerificationWriter.requestSchoolEmailVerification(
                studentVerification,
                10L,
                "luna@snu.ac.kr",
                requestedAt
        );

        // then
        assertThat(result.getStatus()).isEqualTo(VerificationStatus.PENDING);
        assertThat(result.getSchoolEmail()).isEqualTo("luna@snu.ac.kr");
        assertThat(result.getRequestedAt()).isEqualTo(requestedAt);
        verify(emailChallengeRepository).replacePendingChallenges(
                100L,
                StudentVerificationEmailChallengeStatus.PENDING,
                StudentVerificationEmailChallengeStatus.REPLACED
        );
    }

    @Test
    void 이메일_challenge_발급시_원문_코드는_결과로_반환하고_해시만_저장한다() {
        // given
        LocalDateTime expiresAt = LocalDateTime.of(2026, 4, 16, 15, 0);

        when(codeGenerator.generateCode()).thenReturn("123456");
        when(codeGenerator.generateChallengeId()).thenReturn("evc_test");
        when(passwordConfig.encode("123456")).thenReturn("hashed-code");
        when(emailChallengeRepository.save(any(StudentVerificationEmailChallenge.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        StudentVerificationEmailChallengeIssueResult result = studentVerificationWriter.issueEmailChallenge(
                100L,
                "luna@snu.ac.kr",
                expiresAt
        );

        // then
        assertThat(result.code()).isEqualTo("123456");
        assertThat(result.challenge().getChallengeId()).isEqualTo("evc_test");
        assertThat(result.challenge().getEmail()).isEqualTo("luna@snu.ac.kr");
        assertThat(result.challenge().getCodeHash()).isEqualTo("hashed-code");
        assertThat(result.challenge().getExpiresAt()).isEqualTo(expiresAt);
    }
}
