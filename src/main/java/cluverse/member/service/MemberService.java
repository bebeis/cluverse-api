package cluverse.member.service;

import cluverse.member.domain.Member;
import cluverse.member.service.implement.MemberReader;
import cluverse.member.service.implement.MemberWriter;
import cluverse.member.service.request.AddInterestRequest;
import cluverse.member.service.request.AddMajorRequest;
import cluverse.member.service.request.MemberNicknameUpdateRequest;
import cluverse.member.service.request.MemberPasswordUpdateRequest;
import cluverse.member.service.request.UpdateProfileRequest;
import cluverse.member.service.response.MemberInterestResponse;
import cluverse.member.service.response.MemberMajorResponse;
import cluverse.member.service.response.MemberProfileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberReader memberReader;
    private final MemberWriter memberWriter;

    public MemberProfileResponse updateProfile(Long memberId, UpdateProfileRequest request) {
        memberWriter.updateProfile(memberId, request);
        return buildProfileResponse(memberId);
    }

    public MemberProfileResponse updateNickname(Long memberId, MemberNicknameUpdateRequest request) {
        memberWriter.updateNickname(memberId, request.nickname());
        return buildProfileResponse(memberId);
    }

    public MemberProfileResponse updateUniversity(Long memberId, Long universityId) {
        memberWriter.updateUniversity(memberId, universityId);
        return buildProfileResponse(memberId);
    }

    public void updatePassword(Long memberId, MemberPasswordUpdateRequest request) {
        memberWriter.updatePassword(memberId, request);
    }

    public void deleteMember(Long memberId) {
        memberWriter.delete(memberId);
    }

    public MemberMajorResponse addMajor(Long memberId, AddMajorRequest request) {
        return memberReader.readMajor(memberWriter.addMajor(memberId, request));
    }

    public void removeMajor(Long memberId, Long majorId) {
        memberWriter.removeMajor(memberId, majorId);
    }

    public MemberInterestResponse addInterest(Long memberId, AddInterestRequest request) {
        memberWriter.addInterest(memberId, request);
        return memberReader.readInterest(request.interestId());
    }

    public void removeInterest(Long memberId, Long interestId) {
        memberWriter.removeInterest(memberId, interestId);
    }

    public void follow(Long followerId, Long followingId) {
        memberReader.readOrThrow(followingId);
        memberWriter.follow(followerId, followingId);
    }

    public void unfollow(Long followerId, Long followingId) {
        memberWriter.unfollow(followerId, followingId);
    }

    public void block(Long blockerId, Long blockedId) {
        memberReader.readOrThrow(blockedId);
        memberWriter.block(blockerId, blockedId);
    }

    public void unblock(Long blockerId, Long blockedId) {
        memberWriter.unblock(blockerId, blockedId);
    }

    private MemberProfileResponse buildProfileResponse(Long memberId) {
        Member member = memberReader.readWithProfileOrThrow(memberId);
        return MemberProfileResponse.of(
                member,
                member.getProfile(),
                memberReader.readUniversitySummary(member.getUniversityId()),
                false,
                false,
                memberReader.countFollowers(memberId),
                memberReader.countFollowings(memberId),
                memberReader.countPosts(memberId),
                true
        );
    }
}
