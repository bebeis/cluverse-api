package cluverse.member.service.implement;

import cluverse.auth.exception.AuthExceptionMessage;
import cluverse.common.exception.NotFoundException;
import cluverse.common.exception.UnauthorizedException;
import cluverse.member.domain.Member;
import cluverse.member.domain.MemberProfile;
import cluverse.member.domain.MemberProfileField;
import cluverse.member.repository.BlockRepository;
import cluverse.member.repository.FollowRepository;
import cluverse.member.repository.MemberQueryRepository;
import cluverse.member.repository.MemberRepository;
import cluverse.member.service.response.BlockedMemberResponse;
import cluverse.member.service.response.MemberInterestResponse;
import cluverse.member.service.response.MemberProfileResponse;
import cluverse.university.domain.University;
import cluverse.university.repository.UniversityRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberReaderTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private MemberQueryRepository memberQueryRepository;

    @Mock
    private FollowRepository followRepository;

    @Mock
    private BlockRepository blockRepository;

    @Mock
    private UniversityRepository universityRepository;

    @InjectMocks
    private MemberReader memberReader;

    @Test
    void 내_프로필_조회시_비공개_프로필이어도_모든_필드를_볼_수_있다() {
        Member member = createMember(1L, "luna", 10L);
        MemberProfile profile = MemberProfile.create(member);
        profile.update(
                "소개",
                "https://cdn.example.com/profile.png",
                "https://github.com/luna",
                "https://notion.so/luna",
                "https://portfolio.example.com",
                "https://instagram.com/luna",
                "https://blog.example.com",
                false,
                List.of(MemberProfileField.BIO)
        );
        member.initProfile(profile);
        University university = createUniversity(10L, "클루대", "https://cdn.example.com/badge.png");

        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(universityRepository.findById(10L)).thenReturn(Optional.of(university));
        when(followRepository.countByFollowingId(1L)).thenReturn(12L);
        when(followRepository.countByFollowerId(1L)).thenReturn(7L);

        MemberProfileResponse response = memberReader.getProfile(1L, 1L);

        assertThat(response.nickname()).isEqualTo("luna");
        assertThat(response.university().universityName()).isEqualTo("클루대");
        assertThat(response.bio()).isEqualTo("소개");
        assertThat(response.profileImageUrl()).isEqualTo("https://cdn.example.com/profile.png");
        assertThat(response.linkGithub()).isEqualTo("https://github.com/luna");
        assertThat(response.isFollowing()).isFalse();
        assertThat(response.isBlocked()).isFalse();
        assertThat(response.followerCount()).isEqualTo(12L);
        assertThat(response.followingCount()).isEqualTo(7L);
    }

    @Test
    void 상대_프로필_조회시_비공개_프로필은_공개_필드만_노출한다() {
        Member member = createMember(1L, "target", 10L);
        MemberProfile profile = MemberProfile.create(member);
        profile.update(
                "소개",
                "https://cdn.example.com/profile.png",
                "https://github.com/target",
                "https://notion.so/target",
                "https://portfolio.example.com",
                "https://instagram.com/target",
                "https://blog.example.com",
                false,
                List.of(MemberProfileField.UNIVERSITY, MemberProfileField.BIO)
        );
        member.initProfile(profile);
        University university = createUniversity(10L, "클루대", "https://cdn.example.com/badge.png");

        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(universityRepository.findById(10L)).thenReturn(Optional.of(university));
        when(followRepository.existsByFollowerIdAndFollowingId(2L, 1L)).thenReturn(true);
        when(blockRepository.existsByBlockerIdAndBlockedId(2L, 1L)).thenReturn(false);
        when(followRepository.countByFollowingId(1L)).thenReturn(3L);
        when(followRepository.countByFollowerId(1L)).thenReturn(4L);

        MemberProfileResponse response = memberReader.getProfile(2L, 1L);

        assertThat(response.university().universityName()).isEqualTo("클루대");
        assertThat(response.bio()).isEqualTo("소개");
        assertThat(response.profileImageUrl()).isNull();
        assertThat(response.linkGithub()).isNull();
        assertThat(response.visibleFields()).containsExactly(
                MemberProfileField.UNIVERSITY,
                MemberProfileField.BIO
        );
        assertThat(response.isFollowing()).isTrue();
        assertThat(response.isBlocked()).isFalse();
    }

    @Test
    void 차단_목록을_조회할_수_있다() {
        LocalDateTime blockedAt = LocalDateTime.of(2026, 3, 12, 10, 0);
        when(memberQueryRepository.findBlockedMembersByBlockerId(1L)).thenReturn(List.of(
                new MemberQueryRepository.BlockedMemberDTO(
                        2L,
                        "blocked-user",
                        30L,
                        "클루대",
                        "https://cdn.example.com/badge.png",
                        "https://cdn.example.com/profile.png",
                        blockedAt
                )
        ));

        List<BlockedMemberResponse> responses = memberReader.getBlockedMembers(1L);

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().memberId()).isEqualTo(2L);
        assertThat(responses.getFirst().nickname()).isEqualTo("blocked-user");
        assertThat(responses.getFirst().blockedAt()).isEqualTo(blockedAt);
    }

    @Test
    void 관심사_목록을_조회할_수_있다() {
        Member member = createMember(1L, "luna", 10L);
        member.addInterest(100L);
        member.addInterest(200L);

        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

        List<MemberInterestResponse> responses = memberReader.getInterests(1L);

        assertThat(responses).containsExactly(
                new MemberInterestResponse(100L),
                new MemberInterestResponse(200L)
        );
    }

    @Test
    void 학교가_없으면_프로필_조회에_실패한다() {
        Member member = createMember(1L, "luna", 10L);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(universityRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> memberReader.getProfile(1L, 1L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("존재하지 않는 학교입니다.");
    }

    @Test
    void 소셜_회원은_학교가_없어도_프로필을_조회할_수_있다() {
        Member member = Member.createSocialMember("social-user");
        ReflectionTestUtils.setField(member, "id", 1L);

        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(followRepository.countByFollowingId(1L)).thenReturn(0L);
        when(followRepository.countByFollowerId(1L)).thenReturn(0L);

        MemberProfileResponse response = memberReader.getProfile(1L, 1L);

        assertThat(response.memberId()).isEqualTo(1L);
        assertThat(response.university()).isNull();
    }

    @Test
    void 비회원은_프로필을_조회할_수_없다() {
        assertThatThrownBy(() -> memberReader.getProfile(null, 1L))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage(AuthExceptionMessage.UNAUTHORIZED.getMessage());
    }

    private Member createMember(Long memberId, String nickname, Long universityId) {
        Member member = Member.create(nickname, universityId);
        ReflectionTestUtils.setField(member, "id", memberId);
        return member;
    }

    private University createUniversity(Long universityId, String name, String badgeImageUrl) {
        University university = mock(University.class);
        when(university.getId()).thenReturn(universityId);
        when(university.getName()).thenReturn(name);
        when(university.getBadgeImageUrl()).thenReturn(badgeImageUrl);
        return university;
    }
}
