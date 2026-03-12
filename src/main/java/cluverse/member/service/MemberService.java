package cluverse.member.service;

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
        return memberReader.getProfile(viewerId, targetMemberId);
    }

    public MemberProfileResponse updateProfile(Long memberId, UpdateProfileRequest request) {
        return memberWriter.updateProfile(memberId, request);
    }

    @Transactional(readOnly = true)
    public List<MemberMajorResponse> getMajors(Long memberId) {
        return memberReader.getMajors(memberId);
    }

    public MemberMajorResponse addMajor(Long memberId, AddMajorRequest request) {
        return memberWriter.addMajor(memberId, request);
    }

    public void removeMajor(Long memberId, Long majorId) {
        memberWriter.removeMajor(memberId, majorId);
    }

    @Transactional(readOnly = true)
    public List<MemberInterestResponse> getInterests(Long memberId) {
        return memberReader.getInterests(memberId);
    }

    public MemberInterestResponse addInterest(Long memberId, AddInterestRequest request) {
        return memberWriter.addInterest(memberId, request);
    }

    public void removeInterest(Long memberId, Long interestId) {
        memberWriter.removeInterest(memberId, interestId);
    }

    public void follow(Long followerId, Long followingId) {
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
        return memberReader.getBlockedMembers(blockerId);
    }
}
