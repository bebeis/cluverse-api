package cluverse.member.service;

import cluverse.auth.exception.AuthExceptionMessage;
import cluverse.common.exception.UnauthorizedException;
import cluverse.member.domain.Member;
import cluverse.member.domain.MemberProfile;
import cluverse.member.domain.MemberProfileField;
import cluverse.member.service.implement.MemberReader;
import cluverse.member.service.implement.MemberWriter;
import cluverse.member.service.request.AddInterestRequest;
import cluverse.member.service.request.UpdateProfileRequest;
import cluverse.member.service.response.BlockedMemberResponse;
import cluverse.member.service.response.MemberFollowResponse;
import cluverse.member.service.response.MemberInterestResponse;
import cluverse.member.service.response.MemberProfileResponse;
import cluverse.member.service.response.MemberProfileSummaryResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    private MemberReader memberReader;

    @Mock
    private MemberWriter memberWriter;

    @InjectMocks
    private MemberService memberService;

    @Test
    void 내_프로필_조회시_서비스가_응답을_조립한다() {
        Member member = createMember(1L, "luna", 10L);
        MemberProfile profile = MemberProfile.create(member);
        profile.update(
                "소개",
                2024,
                "https://cdn.example.com/profile.png",
                "https://github.com/luna",
                "https://notion.so/luna",
                null,
                null,
                null,
                false,
                List.of(MemberProfileField.BIO)
        );
        member.initProfile(profile);
        MemberProfileSummaryResponse university = new MemberProfileSummaryResponse(
                10L,
                "클루대",
                "https://cdn.example.com/badge.png"
        );

        when(memberReader.readOrThrow(1L)).thenReturn(member);
        when(memberReader.readUniversitySummary(10L)).thenReturn(university);
        when(memberReader.countFollowers(1L)).thenReturn(12L);
        when(memberReader.countFollowings(1L)).thenReturn(7L);
        when(memberReader.countPosts(1L)).thenReturn(34L);

        MemberProfileResponse result = memberService.getProfile(1L, 1L);

        assertThat(result.nickname()).isEqualTo("luna");
        assertThat(result.university().universityName()).isEqualTo("클루대");
        assertThat(result.bio()).isEqualTo("소개");
        assertThat(result.entranceYear()).isEqualTo(2024);
        assertThat(result.profileImageUrl()).isEqualTo("https://cdn.example.com/profile.png");
        assertThat(result.isFollowing()).isFalse();
        assertThat(result.isBlocked()).isFalse();
        assertThat(result.followerCount()).isEqualTo(12L);
        assertThat(result.followingCount()).isEqualTo(7L);
        assertThat(result.postCount()).isEqualTo(34L);
        verify(memberReader).readOrThrow(1L);
        verify(memberReader).readUniversitySummary(10L);
        verify(memberReader).countFollowers(1L);
        verify(memberReader).countFollowings(1L);
        verify(memberReader).countPosts(1L);
        verify(memberReader, never()).isFollowing(1L, 1L);
        verify(memberReader, never()).isBlocked(1L, 1L);
    }

    @Test
    void 상대_프로필_조회시_서비스가_공개_필드만_응답한다() {
        Member member = createMember(1L, "target", 10L);
        MemberProfile profile = MemberProfile.create(member);
        profile.update(
                "소개",
                2023,
                "https://cdn.example.com/profile.png",
                "https://github.com/target",
                "https://notion.so/target",
                null,
                null,
                null,
                false,
                List.of(MemberProfileField.UNIVERSITY, MemberProfileField.ENTRANCE_YEAR, MemberProfileField.BIO)
        );
        member.initProfile(profile);
        MemberProfileSummaryResponse university = new MemberProfileSummaryResponse(
                10L,
                "클루대",
                "https://cdn.example.com/badge.png"
        );

        when(memberReader.readOrThrow(1L)).thenReturn(member);
        when(memberReader.readUniversitySummary(10L)).thenReturn(university);
        when(memberReader.isFollowing(2L, 1L)).thenReturn(true);
        when(memberReader.isBlocked(2L, 1L)).thenReturn(false);
        when(memberReader.countFollowers(1L)).thenReturn(3L);
        when(memberReader.countFollowings(1L)).thenReturn(4L);
        when(memberReader.countPosts(1L)).thenReturn(11L);

        MemberProfileResponse result = memberService.getProfile(2L, 1L);

        assertThat(result.university().universityName()).isEqualTo("클루대");
        assertThat(result.bio()).isEqualTo("소개");
        assertThat(result.entranceYear()).isEqualTo(2023);
        assertThat(result.profileImageUrl()).isNull();
        assertThat(result.linkGithub()).isNull();
        assertThat(result.visibleFields()).containsExactly(
                MemberProfileField.UNIVERSITY,
                MemberProfileField.ENTRANCE_YEAR,
                MemberProfileField.BIO
        );
        assertThat(result.isFollowing()).isTrue();
        assertThat(result.isBlocked()).isFalse();
        assertThat(result.postCount()).isEqualTo(11L);
    }

    @Test
    void 차단_목록_조회는_reader에_위임한다() {
        List<BlockedMemberResponse> responses = List.of(
                new BlockedMemberResponse(2L, "blocked-user", 30L, "클루대", "badge", "profile", null)
        );
        when(memberReader.readBlockedMembers(1L)).thenReturn(responses);

        List<BlockedMemberResponse> result = memberService.getBlockedMembers(1L);

        assertThat(result).isEqualTo(responses);
        verify(memberReader).readBlockedMembers(1L);
    }

    @Test
    void 팔로워_목록_조회는_reader에_위임한다() {
        Member member = createMember(1L, "luna", 10L);
        List<MemberFollowResponse> responses = List.of(
                new MemberFollowResponse(2L, "nova", "https://cdn.example.com/nova.png")
        );

        when(memberReader.readOrThrow(1L)).thenReturn(member);
        when(memberReader.readFollowers(1L)).thenReturn(responses);

        List<MemberFollowResponse> result = memberService.getFollowers(1L);

        assertThat(result).isEqualTo(responses);
        verify(memberReader).readOrThrow(1L);
        verify(memberReader).readFollowers(1L);
    }

    @Test
    void 팔로잉_목록_조회는_reader에_위임한다() {
        Member member = createMember(1L, "luna", 10L);
        List<MemberFollowResponse> responses = List.of(
                new MemberFollowResponse(3L, "sol", "https://cdn.example.com/sol.png")
        );

        when(memberReader.readOrThrow(1L)).thenReturn(member);
        when(memberReader.readFollowings(1L)).thenReturn(responses);

        List<MemberFollowResponse> result = memberService.getFollowings(1L);

        assertThat(result).isEqualTo(responses);
        verify(memberReader).readOrThrow(1L);
        verify(memberReader).readFollowings(1L);
    }

    @Test
    void 관심사_추가시_서비스가_멤버를_조회하고_응답을_만든다() {
        Member member = createMember(1L, "luna", 10L);
        AddInterestRequest request = new AddInterestRequest(300L);
        MemberInterestResponse response = new MemberInterestResponse(300L, "스터디", "ACADEMIC");

        when(memberReader.readOrThrow(1L)).thenReturn(member);
        when(memberReader.readInterest(300L)).thenReturn(response);

        MemberInterestResponse result = memberService.addInterest(1L, request);

        assertThat(result).isEqualTo(response);
        verify(memberReader).readOrThrow(1L);
        verify(memberReader).readInterest(300L);
        verify(memberWriter).addInterest(member, request);
    }

    @Test
    void 프로필_수정시_서비스가_수정후_응답을_다시_조립한다() {
        Member member = Member.createSocialMember("social-user");
        ReflectionTestUtils.setField(member, "id", 1L);
        UpdateProfileRequest request = new UpdateProfileRequest(
                "소개",
                2022,
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                List.of(MemberProfileField.BIO)
        );

        when(memberReader.readOrThrow(1L)).thenReturn(member);
        when(memberReader.readUniversitySummary(null)).thenReturn(null);
        when(memberReader.countFollowers(1L)).thenReturn(0L);
        when(memberReader.countFollowings(1L)).thenReturn(0L);
        when(memberReader.countPosts(1L)).thenReturn(0L);
        doAnswer(invocation -> {
            Member target = invocation.getArgument(0);
            UpdateProfileRequest updateRequest = invocation.getArgument(1);
            MemberProfile profile = MemberProfile.create(target);
            profile.update(
                    updateRequest.bio(),
                    updateRequest.entranceYear(),
                    updateRequest.profileImageUrl(),
                    updateRequest.linkGithub(),
                    updateRequest.linkNotion(),
                    updateRequest.linkPortfolio(),
                    updateRequest.linkInstagram(),
                    updateRequest.linkEtc(),
                    updateRequest.isPublic(),
                    updateRequest.visibleFields()
            );
            target.initProfile(profile);
            return null;
        }).when(memberWriter).updateProfile(any(Member.class), any(UpdateProfileRequest.class));

        MemberProfileResponse result = memberService.updateProfile(1L, request);

        assertThat(result.memberId()).isEqualTo(1L);
        assertThat(result.university()).isNull();
        assertThat(result.bio()).isEqualTo("소개");
        assertThat(result.entranceYear()).isEqualTo(2022);
        assertThat(result.postCount()).isZero();
        verify(memberWriter).updateProfile(member, request);
    }

    @Test
    void 학교_수정시_서비스가_수정후_응답을_다시_조립한다() {
        Member member = Member.createSocialMember("social-user");
        ReflectionTestUtils.setField(member, "id", 1L);
        MemberProfileSummaryResponse university = new MemberProfileSummaryResponse(
                10L,
                "클루대",
                "https://cdn.example.com/badge.png"
        );

        when(memberReader.readOrThrow(1L)).thenReturn(member);
        when(memberReader.readUniversitySummary(10L)).thenReturn(university);
        when(memberReader.countFollowers(1L)).thenReturn(0L);
        when(memberReader.countFollowings(1L)).thenReturn(0L);
        when(memberReader.countPosts(1L)).thenReturn(0L);
        doAnswer(invocation -> {
            Member target = invocation.getArgument(0);
            Long universityId = invocation.getArgument(1);
            target.assignUniversity(universityId);
            return null;
        }).when(memberWriter).updateUniversity(any(Member.class), any(Long.class));

        MemberProfileResponse result = memberService.updateUniversity(1L, 10L);

        assertThat(result.memberId()).isEqualTo(1L);
        assertThat(result.university().universityId()).isEqualTo(10L);
        assertThat(result.university().universityName()).isEqualTo("클루대");
        verify(memberWriter).updateUniversity(member, 10L);
    }

    @Test
    void 비회원은_프로필을_조회할_수_없다() {
        assertThatThrownBy(() -> memberService.getProfile(null, 1L))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage(AuthExceptionMessage.UNAUTHORIZED.getMessage());
    }

    private Member createMember(Long memberId, String nickname, Long universityId) {
        Member member = Member.create(nickname, universityId);
        ReflectionTestUtils.setField(member, "id", memberId);
        return member;
    }
}
