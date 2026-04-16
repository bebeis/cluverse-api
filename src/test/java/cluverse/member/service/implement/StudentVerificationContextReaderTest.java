package cluverse.member.service.implement;

import cluverse.common.config.PasswordConfig;
import cluverse.common.exception.BadRequestException;
import cluverse.member.domain.Member;
import cluverse.member.domain.StudentVerification;
import cluverse.member.domain.StudentVerificationEmailChallenge;
import cluverse.university.domain.University;
import cluverse.university.service.implement.UniversityReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentVerificationContextReaderTest {

    @Mock
    private MemberReader memberReader;

    @Mock
    private UniversityReader universityReader;

    @Mock
    private StudentVerificationReader studentVerificationReader;

    @Mock
    private PasswordConfig passwordConfig;

    @InjectMocks
    private StudentVerificationContextReader studentVerificationContextReader;

    @Test
    void 이메일_challenge_요청_컨텍스트를_검증된_상태로_조회한다() {
        // given
        Member member = createMember(1L, 10L);
        University university = createUniversity(10L, "서울대학교", "snu.ac.kr");
        StudentVerification studentVerification = StudentVerification.create(1L, 10L);

        when(memberReader.readOrThrow(1L)).thenReturn(member);
        when(universityReader.readOrThrow(10L)).thenReturn(university);
        when(studentVerificationReader.findOrCreateByMemberId(1L, 10L)).thenReturn(studentVerification);

        // when
        StudentVerificationEmailChallengeRequestContext context =
                studentVerificationContextReader.readEmailChallengeRequestContext(1L, " LUNA@SNU.AC.KR ");

        // then
        assertThat(context.member()).isSameAs(member);
        assertThat(context.university()).isSameAs(university);
        assertThat(context.studentVerification()).isSameAs(studentVerification);
        assertThat(context.email()).isEqualTo("luna@snu.ac.kr");
    }

    @Test
    void 이메일_challenge_요청시_학교_도메인이_다르면_예외가_발생한다() {
        // given
        Member member = createMember(1L, 10L);
        University university = createUniversity(10L, "서울대학교", "snu.ac.kr");

        when(memberReader.readOrThrow(1L)).thenReturn(member);
        when(universityReader.readOrThrow(10L)).thenReturn(university);

        // when, then
        assertThatThrownBy(() ->
                studentVerificationContextReader.readEmailChallengeRequestContext(1L, "luna@example.com")
        ).isInstanceOf(BadRequestException.class);
    }

    @Test
    void 이메일_confirmation_컨텍스트를_검증하고_시도_횟수를_증가시킨다() {
        // given
        Member member = createMember(1L, 10L);
        StudentVerification studentVerification = createPendingStudentVerification(1L, 10L);
        StudentVerificationEmailChallenge challenge = createPendingChallenge(100L);
        University university = createUniversity(10L, "서울대학교", "snu.ac.kr");
        LocalDateTime now = LocalDateTime.of(2026, 4, 16, 14, 57);

        when(memberReader.readOrThrow(1L)).thenReturn(member);
        when(studentVerificationReader.readEmailChallengeOrThrow("evc_test")).thenReturn(challenge);
        when(studentVerificationReader.readOrThrow(100L)).thenReturn(studentVerification);
        when(studentVerificationReader.isLatestPendingEmailChallenge(challenge)).thenReturn(true);
        when(passwordConfig.matches("123456", "hashed-code")).thenReturn(true);
        when(universityReader.readOrThrow(10L)).thenReturn(university);

        // when
        StudentVerificationEmailConfirmationContext context =
                studentVerificationContextReader.readEmailConfirmationContext(
                        1L,
                        "evc_test",
                        "123456",
                        now
                );

        // then
        assertThat(context.member()).isSameAs(member);
        assertThat(context.university()).isSameAs(university);
        assertThat(context.studentVerification()).isSameAs(studentVerification);
        assertThat(context.challenge()).isSameAs(challenge);
        assertThat(challenge.getAttemptCount()).isEqualTo(1);
    }

    @Test
    void 이메일_confirmation_요청시_코드가_틀리면_예외가_발생한다() {
        // given
        Member member = createMember(1L, 10L);
        StudentVerification studentVerification = createPendingStudentVerification(1L, 10L);
        StudentVerificationEmailChallenge challenge = createPendingChallenge(100L);
        LocalDateTime now = LocalDateTime.of(2026, 4, 16, 14, 57);

        when(memberReader.readOrThrow(1L)).thenReturn(member);
        when(studentVerificationReader.readEmailChallengeOrThrow("evc_test")).thenReturn(challenge);
        when(studentVerificationReader.readOrThrow(100L)).thenReturn(studentVerification);
        when(studentVerificationReader.isLatestPendingEmailChallenge(challenge)).thenReturn(true);
        when(passwordConfig.matches("000000", "hashed-code")).thenReturn(false);

        // when, then
        assertThatThrownBy(() -> studentVerificationContextReader.readEmailConfirmationContext(
                1L,
                "evc_test",
                "000000",
                now
        )).isInstanceOf(BadRequestException.class);
    }

    @Test
    void 이메일_confirmation_요청시_만료된_challenge면_상태를_만료로_바꾸고_예외가_발생한다() {
        // given
        Member member = createMember(1L, 10L);
        StudentVerification studentVerification = createPendingStudentVerification(1L, 10L);
        StudentVerificationEmailChallenge challenge = StudentVerificationEmailChallenge.create(
                100L,
                "evc_test",
                "luna@snu.ac.kr",
                "hashed-code",
                LocalDateTime.of(2026, 4, 16, 14, 55)
        );
        LocalDateTime now = LocalDateTime.of(2026, 4, 16, 14, 55);

        when(memberReader.readOrThrow(1L)).thenReturn(member);
        when(studentVerificationReader.readEmailChallengeOrThrow("evc_test")).thenReturn(challenge);
        when(studentVerificationReader.readOrThrow(100L)).thenReturn(studentVerification);
        when(studentVerificationReader.isLatestPendingEmailChallenge(challenge)).thenReturn(true);

        // when, then
        assertThatThrownBy(() -> studentVerificationContextReader.readEmailConfirmationContext(
                1L,
                "evc_test",
                "123456",
                now
        )).isInstanceOf(BadRequestException.class);
        assertThat(challenge.isExpiredStatus()).isTrue();
    }

    private Member createMember(Long memberId, Long universityId) {
        Member member = Member.create("luna", universityId);
        ReflectionTestUtils.setField(member, "id", memberId);
        return member;
    }

    private StudentVerification createPendingStudentVerification(Long memberId, Long universityId) {
        StudentVerification studentVerification = StudentVerification.create(memberId, universityId);
        ReflectionTestUtils.setField(studentVerification, "id", 100L);
        studentVerification.requestSchoolEmailVerification(
                universityId,
                "luna@snu.ac.kr",
                LocalDateTime.of(2026, 4, 16, 14, 55)
        );
        return studentVerification;
    }

    private StudentVerificationEmailChallenge createPendingChallenge(Long studentVerificationId) {
        return StudentVerificationEmailChallenge.create(
                studentVerificationId,
                "evc_test",
                "luna@snu.ac.kr",
                "hashed-code",
                LocalDateTime.of(2026, 4, 16, 15, 0)
        );
    }

    private University createUniversity(Long universityId, String name, String emailDomain) {
        University university = University.create(name, emailDomain, null, null, true);
        ReflectionTestUtils.setField(university, "id", universityId);
        return university;
    }
}
