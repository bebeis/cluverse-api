package cluverse.member.service.implement;

import cluverse.common.config.PasswordConfig;
import cluverse.common.exception.BadRequestException;
import cluverse.interest.repository.InterestRepository;
import cluverse.major.repository.MajorRepository;
import cluverse.member.domain.Member;
import cluverse.member.domain.MemberProfileField;
import cluverse.member.repository.MemberRepository;
import cluverse.member.service.request.AddInterestRequest;
import cluverse.member.service.request.MemberPasswordUpdateRequest;
import cluverse.member.service.request.UpdateProfileRequest;
import cluverse.university.service.implement.UniversityReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberWriterTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private MajorRepository majorRepository;

    @Mock
    private InterestRepository interestRepository;

    @Mock
    private MemberReader memberReader;

    @Mock
    private UniversityReader universityReader;

    @Mock
    private PasswordConfig passwordConfig;

    @InjectMocks
    private MemberWriter memberWriter;

    @Test
    void 관심사를_추가할_수_있다() {
        Member member = createMember(1L, "luna");
        when(interestRepository.existsById(300L)).thenReturn(true);
        when(memberReader.readOrThrow(1L)).thenReturn(member);

        memberWriter.addInterest(1L, new AddInterestRequest(300L));

        assertThat(member.getInterests()).containsExactly(300L);
    }

    @Test
    void 소셜_회원도_학교_없이_프로필을_수정할_수_있다() {
        Member member = Member.createSocialMember("social-user");
        ReflectionTestUtils.setField(member, "id", 1L);
        when(memberReader.readOrThrow(1L)).thenReturn(member);

        memberWriter.updateProfile(
                1L,
                new UpdateProfileRequest(
                        "소개",
                        2024,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        false,
                        List.of(MemberProfileField.BIO)
                )
        );

        assertThat(member.getProfile()).isNotNull();
        assertThat(member.getProfile().getBio()).isEqualTo("소개");
    }

    @Test
    void 닉네임을_수정할_수_있다() {
        Member member = createMember(1L, "luna");
        when(memberReader.readOrThrow(1L)).thenReturn(member);
        when(memberRepository.existsByNickname("nova")).thenReturn(false);

        memberWriter.updateNickname(1L, "nova");

        assertThat(member.getNickname()).isEqualTo("nova");
    }

    @Test
    void 이미_사용_중인_닉네임으로는_수정할_수_없다() {
        Member member = createMember(1L, "luna");
        when(memberReader.readOrThrow(1L)).thenReturn(member);
        when(memberRepository.existsByNickname("nova")).thenReturn(true);

        assertThatThrownBy(() -> memberWriter.updateNickname(1L, "nova"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("이미 사용 중인 닉네임입니다.");
    }

    @Test
    void 학교를_수정하면_존재를_검증하고_배정한다() {
        Member member = createMember(1L, "luna");
        when(memberReader.readOrThrow(1L)).thenReturn(member);

        memberWriter.updateUniversity(1L, 20L);

        assertThat(member.getUniversityId()).isEqualTo(20L);
    }

    @Test
    void 비밀번호_수정시_현재_비밀번호를_검증한_후_변경한다() {
        Member member = createMember(1L, "luna");
        member.initMemberAuth("luna@example.com", "encoded-old-password");
        MemberPasswordUpdateRequest request = new MemberPasswordUpdateRequest("old-password", "new-password");

        when(memberReader.readOrThrow(1L)).thenReturn(member);
        when(passwordConfig.matches("old-password", "encoded-old-password")).thenReturn(true);
        when(passwordConfig.encode("new-password")).thenReturn("encoded-new-password");

        memberWriter.updatePassword(1L, request);

        assertThat(member.getMemberAuth().getPasswordHash()).isEqualTo("encoded-new-password");
    }

    @Test
    void 현재_비밀번호가_틀리면_비밀번호를_변경할_수_없다() {
        Member member = createMember(1L, "luna");
        member.initMemberAuth("luna@example.com", "encoded-old-password");
        MemberPasswordUpdateRequest request = new MemberPasswordUpdateRequest("wrong-password", "new-password");

        when(memberReader.readOrThrow(1L)).thenReturn(member);
        when(passwordConfig.matches("wrong-password", "encoded-old-password")).thenReturn(false);

        assertThatThrownBy(() -> memberWriter.updatePassword(1L, request))
                .isInstanceOf(BadRequestException.class);
    }

    private Member createMember(Long memberId, String nickname) {
        Member member = Member.create(nickname, 10L);
        ReflectionTestUtils.setField(member, "id", memberId);
        return member;
    }
}
