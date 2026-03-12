package cluverse.member.service;

import cluverse.auth.exception.AuthExceptionMessage;
import cluverse.common.exception.UnauthorizedException;
import cluverse.member.domain.Member;
import cluverse.member.service.implement.MemberReader;
import cluverse.member.service.implement.MemberWriter;
import cluverse.member.service.request.AddInterestRequest;
import cluverse.member.service.request.AddMajorRequest;
import cluverse.member.service.request.UpdateProfileRequest;
import cluverse.member.service.response.BlockedMemberResponse;
import cluverse.member.service.response.MemberInterestResponse;
import cluverse.member.service.response.MemberMajorResponse;
import cluverse.member.service.response.MemberProfileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberService {

    private final MemberReader memberReader;
    private final MemberWriter memberWriter;

    @Transactional(readOnly = true)
    public MemberProfileResponse getProfile(Long viewerId, Long targetMemberId) {
        validateViewerId(viewerId);
        Member member = memberReader.readOrThrow(targetMemberId);
        return buildProfileResponse(viewerId, member);
    }

    public MemberProfileResponse updateProfile(Long memberId, UpdateProfileRequest request) {
        Member member = memberReader.readOrThrow(memberId);
        memberWriter.updateProfile(member, request);
        return buildProfileResponse(memberId, member);
    }

    @Transactional(readOnly = true)
    public List<MemberMajorResponse> getMajors(Long memberId) {
        return memberReader.readMajors(memberId);
    }

    public MemberMajorResponse addMajor(Long memberId, AddMajorRequest request) {
        Member member = memberReader.readOrThrow(memberId);
        return MemberMajorResponse.from(memberWriter.addMajor(member, request));
    }

    public void removeMajor(Long memberId, Long majorId) {
        Member member = memberReader.readOrThrow(memberId);
        memberWriter.removeMajor(member, majorId);
    }

    @Transactional(readOnly = true)
    public List<MemberInterestResponse> getInterests(Long memberId) {
        return memberReader.readInterests(memberId);
    }

    public MemberInterestResponse addInterest(Long memberId, AddInterestRequest request) {
        Member member = memberReader.readOrThrow(memberId);
        memberWriter.addInterest(member, request);
        return MemberInterestResponse.from(request.interestId());
    }

    public void removeInterest(Long memberId, Long interestId) {
        Member member = memberReader.readOrThrow(memberId);
        memberWriter.removeInterest(member, interestId);
    }

    public void follow(Long followerId, Long followingId) {
        memberReader.readOrThrow(followingId);
        memberWriter.follow(followerId, followingId);
    }

    public void unfollow(Long followerId, Long followingId) {
        memberWriter.unfollow(followerId, followingId);
    }

    @Transactional(readOnly = true)
    public boolean isFollowing(Long followerId, Long followingId) {
        return memberReader.isFollowing(followerId, followingId);
    }

    public void block(Long blockerId, Long blockedId) {
        memberReader.readOrThrow(blockedId);
        memberWriter.block(blockerId, blockedId);
    }

    public void unblock(Long blockerId, Long blockedId) {
        memberWriter.unblock(blockerId, blockedId);
    }

    @Transactional(readOnly = true)
    public boolean isBlocked(Long blockerId, Long blockedId) {
        return memberReader.isBlocked(blockerId, blockedId);
    }

    @Transactional(readOnly = true)
    public List<BlockedMemberResponse> getBlockedMembers(Long blockerId) {
        return memberReader.readBlockedMembers(blockerId);
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
                sameMember
        );
    }

    private void validateViewerId(Long viewerId) {
        if (viewerId == null) {
            throw new UnauthorizedException(AuthExceptionMessage.UNAUTHORIZED.getMessage());
        }
    }
}
