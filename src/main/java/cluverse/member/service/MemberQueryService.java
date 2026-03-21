package cluverse.member.service;

import cluverse.auth.exception.AuthExceptionMessage;
import cluverse.common.exception.UnauthorizedException;
import cluverse.member.domain.Member;
import cluverse.member.service.implement.MemberReader;
import cluverse.member.service.request.AddMajorRequest;
import cluverse.member.service.response.BlockedMemberResponse;
import cluverse.member.service.response.MemberFollowResponse;
import cluverse.member.service.response.MemberInterestResponse;
import cluverse.member.service.response.MemberMajorResponse;
import cluverse.member.service.response.MemberNicknameAvailabilityResponse;
import cluverse.member.service.response.MemberProfileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberQueryService {

    private final MemberReader memberReader;

    public MemberProfileResponse getProfile(Long viewerId, Long targetMemberId) {
        validateViewerId(viewerId);
        Member member = memberReader.readOrThrow(targetMemberId);
        return buildProfileResponse(viewerId, member);
    }

    public MemberNicknameAvailabilityResponse checkNicknameAvailability(String nickname) {
        return new MemberNicknameAvailabilityResponse(nickname, !memberReader.existsByNickname(nickname));
    }

    public List<MemberMajorResponse> getMajors(Long memberId) {
        return memberReader.readMajors(memberId);
    }

    public List<MemberInterestResponse> getInterests(Long memberId) {
        return memberReader.readInterests(memberId);
    }

    public boolean isFollowing(Long followerId, Long followingId) {
        return memberReader.isFollowing(followerId, followingId);
    }

    public boolean isBlocked(Long blockerId, Long blockedId) {
        return memberReader.isBlocked(blockerId, blockedId);
    }

    public boolean isAdmin(Long memberId) {
        return memberReader.readOrThrow(memberId).isAdmin();
    }

    public boolean isVerified(Long memberId) {
        if (memberId == null) {
            return false;
        }
        return memberReader.readOrThrow(memberId).isVerified();
    }

    public List<BlockedMemberResponse> getBlockedMembers(Long blockerId) {
        return memberReader.readBlockedMembers(blockerId);
    }

    public List<MemberFollowResponse> getFollowers(Long memberId) {
        memberReader.readOrThrow(memberId);
        return memberReader.readFollowers(memberId);
    }

    public List<MemberFollowResponse> getFollowings(Long memberId) {
        memberReader.readOrThrow(memberId);
        return memberReader.readFollowings(memberId);
    }

    public Map<Long, Member> readMemberMap(Collection<Long> memberIds) {
        return memberReader.readMemberMap(memberIds);
    }

    private MemberProfileResponse buildProfileResponse(Long viewerId, Member member) {
        boolean sameMember = viewerId.equals(member.getId());
        return MemberProfileResponse.of(
                member,
                member.getProfile(),
                memberReader.readUniversitySummary(member.getUniversityId()),
                !sameMember && memberReader.isFollowing(viewerId, member.getId()),
                !sameMember && memberReader.isBlocked(viewerId, member.getId()),
                memberReader.countFollowers(member.getId()),
                memberReader.countFollowings(member.getId()),
                memberReader.countPosts(member.getId()),
                sameMember
        );
    }

    private void validateViewerId(Long viewerId) {
        if (viewerId == null) {
            throw new UnauthorizedException(AuthExceptionMessage.UNAUTHORIZED.getMessage());
        }
    }
}
