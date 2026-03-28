package cluverse.member.service;

import cluverse.common.config.PasswordConfig;
import cluverse.common.exception.BadRequestException;
import cluverse.member.domain.Member;
import cluverse.member.domain.MemberAuth;
import cluverse.member.exception.MemberExceptionMessage;
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
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberService {

    private final MemberReader memberReader;
    private final MemberWriter memberWriter;
    private final PasswordConfig passwordConfig;

    public MemberProfileResponse updateProfile(Long memberId, UpdateProfileRequest request) {
        Member member = memberReader.readOrThrow(memberId);
        memberWriter.updateProfile(member, request);
        return buildProfileResponse(memberId, member);
    }

    public MemberProfileResponse updateNickname(Long memberId, MemberNicknameUpdateRequest request) {
        Member member = memberReader.readOrThrow(memberId);
        memberWriter.updateNickname(member, request.nickname());
        return buildProfileResponse(memberId, member);
    }

    public MemberProfileResponse updateUniversity(Long memberId, Long universityId) {
        Member member = memberReader.readOrThrow(memberId);
        memberWriter.updateUniversity(member, universityId);
        return buildProfileResponse(memberId, member);
    }

    public void updatePassword(Long memberId, MemberPasswordUpdateRequest request) {
        Member member = memberReader.readOrThrow(memberId);
        MemberAuth memberAuth = validatePasswordChangable(member);
        validateCurrentPassword(memberAuth, request.currentPassword());
        memberWriter.updatePassword(member, passwordConfig.encode(request.newPassword()));
    }

    public void deleteMember(Long memberId) {
        Member member = memberReader.readOrThrow(memberId);
        memberWriter.delete(member);
    }

    public MemberMajorResponse addMajor(Long memberId, AddMajorRequest request) {
        Member member = memberReader.readOrThrow(memberId);
        return memberReader.readMajor(memberWriter.addMajor(member, request).getId());
    }

    public void removeMajor(Long memberId, Long majorId) {
        Member member = memberReader.readOrThrow(memberId);
        memberWriter.removeMajor(member, majorId);
    }

    public MemberInterestResponse addInterest(Long memberId, AddInterestRequest request) {
        Member member = memberReader.readOrThrow(memberId);
        memberWriter.addInterest(member, request);
        return memberReader.readInterest(request.interestId());
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

    public void block(Long blockerId, Long blockedId) {
        memberReader.readOrThrow(blockedId);
        memberWriter.block(blockerId, blockedId);
    }

    public void unblock(Long blockerId, Long blockedId) {
        memberWriter.unblock(blockerId, blockedId);
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

    private MemberAuth validatePasswordChangable(Member member) {
        MemberAuth memberAuth = member.getMemberAuth();
        if (memberAuth == null || memberAuth.getPasswordHash() == null) {
            throw new BadRequestException(MemberExceptionMessage.PASSWORD_CHANGE_NOT_ALLOWED.getMessage());
        }
        return memberAuth;
    }

    private void validateCurrentPassword(MemberAuth memberAuth, String currentPassword) {
        if (!passwordConfig.matches(currentPassword, memberAuth.getPasswordHash())) {
            throw new BadRequestException(MemberExceptionMessage.INVALID_PASSWORD.getMessage());
        }
    }
}
