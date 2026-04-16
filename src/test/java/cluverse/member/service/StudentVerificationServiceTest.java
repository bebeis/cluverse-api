package cluverse.member.service;

import cluverse.common.exception.BadRequestException;
import cluverse.member.client.StudentVerificationEmailClient;
import cluverse.member.domain.Member;
import cluverse.member.domain.StudentVerification;
import cluverse.member.domain.StudentVerificationEmailChallenge;
import cluverse.member.domain.VerificationStatus;
import cluverse.member.service.implement.StudentVerificationContextReader;
import cluverse.member.service.implement.StudentVerificationEmailChallengeIssueResult;
import cluverse.member.service.implement.StudentVerificationEmailChallengeRequestContext;
import cluverse.member.service.implement.StudentVerificationEmailConfirmationContext;
import cluverse.member.service.implement.StudentVerificationWriter;
import cluverse.member.service.request.StudentVerificationEmailChallengeCreateRequest;
import cluverse.member.service.request.StudentVerificationEmailConfirmationCreateRequest;
import cluverse.member.service.response.StudentVerificationEmailChallengeResponse;
import cluverse.member.service.response.StudentVerificationStatusResponse;
import cluverse.university.domain.University;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentVerificationServiceTest {

    @Mock
    private StudentVerificationContextReader studentVerificationContextReader;

    @Mock
    private StudentVerificationWriter studentVerificationWriter;

    @Mock
    private StudentVerificationEmailClient emailClient;

    @InjectMocks
    private StudentVerificationService studentVerificationService;

    @Test
    void 이메일_challenge_생성시_검증된_컨텍스트로_인증_요청을_저장하고_메일을_발송한다() {
        // given
        Member member = createMember(1L, 10L);
        University university = createUniversity(10L, "서울대학교", "snu.ac.kr");
        StudentVerification studentVerification = StudentVerification.create(1L, 10L);
        ReflectionTestUtils.setField(studentVerification, "id", 100L);
        StudentVerificationEmailChallenge challenge = StudentVerificationEmailChallenge.create(
                100L,
                "evc_test",
                "luna@snu.ac.kr",
                "hashed-code",
                LocalDateTime.of(2026, 4, 16, 15, 0)
        );
        StudentVerificationEmailChallengeCreateRequest request =
                new StudentVerificationEmailChallengeCreateRequest("LUNA@snu.ac.kr");

        when(studentVerificationContextReader.readEmailChallengeRequestContext(1L, "LUNA@snu.ac.kr"))
                .thenReturn(new StudentVerificationEmailChallengeRequestContext(
                        member,
                        university,
                        "luna@snu.ac.kr",
                        studentVerification
                ));
        when(studentVerificationWriter.requestSchoolEmailVerification(
                eq(studentVerification),
                eq(10L),
                eq("luna@snu.ac.kr"),
                any(LocalDateTime.class)
        )).thenAnswer(invocation -> {
            studentVerification.requestSchoolEmailVerification(
                    10L,
                    "luna@snu.ac.kr",
                    invocation.getArgument(3)
            );
            return studentVerification;
        });
        when(studentVerificationWriter.issueEmailChallenge(
                eq(100L),
                eq("luna@snu.ac.kr"),
                any(LocalDateTime.class)
        )).thenReturn(new StudentVerificationEmailChallengeIssueResult("123456", challenge));

        // when
        StudentVerificationEmailChallengeResponse response =
                studentVerificationService.createEmailChallenge(1L, request);

        // then
        assertThat(response.challengeId()).isEqualTo("evc_test");
        assertThat(response.email()).isEqualTo("luna@snu.ac.kr");
        assertThat(response.verificationStatus()).isEqualTo(VerificationStatus.PENDING);
        assertThat(member.getVerificationStatus()).isEqualTo(VerificationStatus.PENDING);
        verify(emailClient).sendVerificationCode(
                eq("luna@snu.ac.kr"),
                eq("123456"),
                any(LocalDateTime.class)
        );
    }

    @Test
    void 이메일_challenge_생성시_컨텍스트_조회에서_예외가_발생하면_메일을_발송하지_않는다() {
        // given
        StudentVerificationEmailChallengeCreateRequest request =
                new StudentVerificationEmailChallengeCreateRequest("luna@example.com");

        when(studentVerificationContextReader.readEmailChallengeRequestContext(1L, "luna@example.com"))
                .thenThrow(new BadRequestException("학교 이메일 도메인이 일치하지 않습니다."));

        // when, then
        assertThatThrownBy(() -> studentVerificationService.createEmailChallenge(1L, request))
                .isInstanceOf(BadRequestException.class);
        verify(emailClient, never()).sendVerificationCode(any(), any(), any(LocalDateTime.class));
    }

    @Test
    void 이메일_confirmation_생성시_검증된_컨텍스트로_학생_인증을_승인한다() {
        // given
        Member member = createMember(1L, 10L);
        member.requestVerification();
        University university = createUniversity(10L, "서울대학교", "snu.ac.kr");
        StudentVerification studentVerification = StudentVerification.create(1L, 10L);
        ReflectionTestUtils.setField(studentVerification, "id", 100L);
        studentVerification.requestSchoolEmailVerification(
                10L,
                "luna@snu.ac.kr",
                LocalDateTime.of(2026, 4, 16, 14, 55)
        );
        StudentVerificationEmailChallenge challenge = StudentVerificationEmailChallenge.create(
                100L,
                "evc_test",
                "luna@snu.ac.kr",
                "hashed-code",
                LocalDateTime.now().plusMinutes(5)
        );
        StudentVerificationEmailConfirmationCreateRequest request =
                new StudentVerificationEmailConfirmationCreateRequest("123456");

        when(studentVerificationContextReader.readEmailConfirmationContext(
                eq(1L),
                eq("evc_test"),
                eq("123456"),
                any(LocalDateTime.class)
        )).thenReturn(new StudentVerificationEmailConfirmationContext(
                member,
                university,
                studentVerification,
                challenge
        ));

        // when
        StudentVerificationStatusResponse response =
                studentVerificationService.createEmailConfirmation(1L, "evc_test", request);

        // then
        assertThat(response.verificationStatus()).isEqualTo(VerificationStatus.APPROVED);
        assertThat(response.verified()).isTrue();
        assertThat(studentVerification.isVerified()).isTrue();
        assertThat(challenge.isVerified()).isTrue();
        assertThat(member.isVerified()).isTrue();
    }

    private Member createMember(Long memberId, Long universityId) {
        Member member = Member.create("luna", universityId);
        ReflectionTestUtils.setField(member, "id", memberId);
        return member;
    }

    private University createUniversity(Long universityId, String name, String emailDomain) {
        University university = University.create(name, emailDomain, null, null, true);
        ReflectionTestUtils.setField(university, "id", universityId);
        return university;
    }
}
