package cluverse.member.service.implement;

import cluverse.common.exception.BadRequestException;
import cluverse.interest.repository.InterestRepository;
import cluverse.major.repository.MajorRepository;
import cluverse.member.domain.Member;
import cluverse.member.domain.MemberProfileField;
import cluverse.member.repository.MemberRepository;
import cluverse.member.service.request.AddInterestRequest;
import cluverse.member.service.request.UpdateProfileRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberWriterTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private MajorRepository majorRepository;

    @Mock
    private InterestRepository interestRepository;

    @InjectMocks
    private MemberWriter memberWriter;

    @Test
    void 관심사를_추가할_수_있다() {
        Member member = Member.create("luna", 10L);
        ReflectionTestUtils.setField(member, "id", 1L);

        when(interestRepository.existsById(300L)).thenReturn(true);

        memberWriter.addInterest(member, new AddInterestRequest(300L));

        assertThat(member.getInterests()).containsExactly(300L);
    }

    @Test
    void 소셜_회원도_학교_없이_프로필을_수정할_수_있다() {
        Member member = Member.createSocialMember("social-user");
        ReflectionTestUtils.setField(member, "id", 1L);

        memberWriter.updateProfile(
                member,
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
        Member member = Member.create("luna", 10L);
        ReflectionTestUtils.setField(member, "id", 1L);

        when(memberRepository.existsByNickname("nova")).thenReturn(false);

        memberWriter.updateNickname(member, "nova");

        assertThat(member.getNickname()).isEqualTo("nova");
    }

    @Test
    void 이미_사용_중인_닉네임으로는_수정할_수_없다() {
        Member member = Member.create("luna", 10L);
        ReflectionTestUtils.setField(member, "id", 1L);

        when(memberRepository.existsByNickname("nova")).thenReturn(true);

        assertThatThrownBy(() -> memberWriter.updateNickname(member, "nova"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("이미 사용 중인 닉네임입니다.");
    }
}
