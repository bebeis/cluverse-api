package cluverse.member.service;

import cluverse.member.domain.Member;
import cluverse.member.domain.StudentVerification;
import cluverse.member.domain.StudentVerificationMethod;
import cluverse.member.domain.VerificationStatus;
import cluverse.member.service.implement.MemberReader;
import cluverse.member.service.implement.StudentVerificationReader;
import cluverse.member.service.response.StudentVerificationStatusResponse;
import cluverse.university.domain.University;
import cluverse.university.service.implement.UniversityReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentVerificationQueryServiceTest {

    @Mock
    private MemberReader memberReader;

    @Mock
    private StudentVerificationReader studentVerificationReader;

    @Mock
    private UniversityReader universityReader;

    @InjectMocks
    private StudentVerificationQueryService studentVerificationQueryService;

    @Test
    void 학생_인증_상태가_있으면_student_verification_기준으로_응답한다() {
        // given
        Member member = createMember(1L, 10L);
        StudentVerification studentVerification = StudentVerification.create(1L, 10L);
        studentVerification.requestSchoolEmailVerification(
                10L,
                "luna@snu.ac.kr",
                LocalDateTime.of(2026, 4, 16, 14, 55)
        );
        University university = createUniversity(10L, "서울대학교", "snu.ac.kr");

        when(memberReader.readOrThrow(1L)).thenReturn(member);
        when(studentVerificationReader.findByMemberId(1L)).thenReturn(Optional.of(studentVerification));
        when(universityReader.readOrThrow(10L)).thenReturn(university);

        // when
        StudentVerificationStatusResponse response =
                studentVerificationQueryService.getVerificationStatus(1L);

        // then
        assertThat(response.verificationStatus()).isEqualTo(VerificationStatus.PENDING);
        assertThat(response.verificationMethod()).isEqualTo(StudentVerificationMethod.SCHOOL_EMAIL);
        assertThat(response.schoolEmail()).isEqualTo("luna@snu.ac.kr");
        assertThat(response.university().emailDomain()).isEqualTo("snu.ac.kr");
    }

    @Test
    void 학생_인증_상태가_없으면_member_호환_컬럼_기준으로_응답한다() {
        // given
        Member member = createMember(1L, 10L);
        University university = createUniversity(10L, "서울대학교", "snu.ac.kr");

        when(memberReader.readOrThrow(1L)).thenReturn(member);
        when(studentVerificationReader.findByMemberId(1L)).thenReturn(Optional.empty());
        when(universityReader.readOrThrow(10L)).thenReturn(university);

        // when
        StudentVerificationStatusResponse response =
                studentVerificationQueryService.getVerificationStatus(1L);

        // then
        assertThat(response.verificationStatus()).isEqualTo(VerificationStatus.NONE);
        assertThat(response.verificationMethod()).isNull();
        assertThat(response.schoolEmail()).isNull();
        assertThat(response.university().universityName()).isEqualTo("서울대학교");
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
