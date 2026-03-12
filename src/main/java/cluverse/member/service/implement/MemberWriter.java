package cluverse.member.service.implement;

import cluverse.common.exception.BadRequestException;
import cluverse.common.exception.NotFoundException;
import cluverse.interest.repository.InterestRepository;
import cluverse.major.repository.MajorRepository;
import cluverse.member.domain.Block;
import cluverse.member.domain.Follow;
import cluverse.member.domain.Member;
import cluverse.member.domain.MemberMajor;
import cluverse.member.domain.MemberProfile;
import cluverse.member.exception.MemberExceptionMessage;
import cluverse.member.repository.BlockRepository;
import cluverse.member.repository.FollowRepository;
import cluverse.member.service.request.AddInterestRequest;
import cluverse.member.service.request.AddMajorRequest;
import cluverse.member.service.request.UpdateProfileRequest;
import cluverse.member.service.response.MemberInterestResponse;
import cluverse.member.service.response.MemberMajorResponse;
import cluverse.member.service.response.MemberProfileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class MemberWriter {

    private final MemberReader memberReader;
    private final FollowRepository followRepository;
    private final BlockRepository blockRepository;
    private final MajorRepository majorRepository;
    private final InterestRepository interestRepository;

    public MemberProfileResponse updateProfile(Long memberId, UpdateProfileRequest request) {
        Member member = memberReader.readOrThrow(memberId);
        MemberProfile profile = member.getProfile();
        if (profile == null) {
            profile = MemberProfile.create(member);
            member.initProfile(profile);
        }
        profile.update(
                request.bio(),
                request.profileImageUrl(),
                request.linkGithub(),
                request.linkNotion(),
                request.linkPortfolio(),
                request.linkInstagram(),
                request.linkEtc(),
                request.isPublic(),
                request.visibleFields()
        );
        return MemberProfileResponse.of(
                member,
                profile,
                memberReader.readUniversitySummary(member.getUniversityId()),
                false,
                false,
                followRepository.countByFollowingId(memberId),
                followRepository.countByFollowerId(memberId),
                true
        );
    }

    public MemberMajorResponse addMajor(Long memberId, AddMajorRequest request) {
        validateMajorExists(request.majorId());
        Member member = memberReader.readOrThrow(memberId);
        member.addMajor(request.majorId(), request.majorType());
        MemberMajor added = member.getMajors().stream()
                .filter(major -> major.getMajorId().equals(request.majorId()))
                .findFirst()
                .orElseThrow();
        return MemberMajorResponse.from(added);
    }

    public void removeMajor(Long memberId, Long majorId) {
        validateMajorExists(majorId);
        memberReader.readOrThrow(memberId).removeMajor(majorId);
    }

    public MemberInterestResponse addInterest(Long memberId, AddInterestRequest request) {
        validateInterestExists(request.interestId());
        Member member = memberReader.readOrThrow(memberId);
        member.addInterest(request.interestId());
        return MemberInterestResponse.from(request.interestId());
    }

    public void removeInterest(Long memberId, Long interestId) {
        validateInterestExists(interestId);
        memberReader.readOrThrow(memberId).removeInterest(interestId);
    }

    public void follow(Long followerId, Long followingId) {
        validateCannotTargetSelf(followerId, followingId, MemberExceptionMessage.CANNOT_FOLLOW_SELF);
        memberReader.readOrThrow(followingId);
        validateNotAlreadyFollowing(followerId, followingId);
        followRepository.save(Follow.of(followerId, followingId));
    }

    public void unfollow(Long followerId, Long followingId) {
        Follow follow = followRepository.findByFollowerIdAndFollowingId(followerId, followingId)
                .orElseThrow(() -> new BadRequestException(MemberExceptionMessage.NOT_FOLLOWING.getMessage()));
        followRepository.delete(follow);
    }

    public void block(Long blockerId, Long blockedId) {
        validateCannotTargetSelf(blockerId, blockedId, MemberExceptionMessage.CANNOT_BLOCK_SELF);
        memberReader.readOrThrow(blockedId);
        validateNotAlreadyBlocked(blockerId, blockedId);
        blockRepository.save(Block.of(blockerId, blockedId));
    }

    public void unblock(Long blockerId, Long blockedId) {
        Block block = blockRepository.findByBlockerIdAndBlockedId(blockerId, blockedId)
                .orElseThrow(() -> new BadRequestException(MemberExceptionMessage.NOT_BLOCKED.getMessage()));
        blockRepository.delete(block);
    }

    private void validateCannotTargetSelf(Long actorId, Long targetId, MemberExceptionMessage message) {
        if (actorId.equals(targetId)) {
            throw new BadRequestException(message.getMessage());
        }
    }

    private void validateNotAlreadyFollowing(Long followerId, Long followingId) {
        if (followRepository.existsByFollowerIdAndFollowingId(followerId, followingId)) {
            throw new BadRequestException(MemberExceptionMessage.ALREADY_FOLLOWING.getMessage());
        }
    }

    private void validateNotAlreadyBlocked(Long blockerId, Long blockedId) {
        if (blockRepository.existsByBlockerIdAndBlockedId(blockerId, blockedId)) {
            throw new BadRequestException(MemberExceptionMessage.ALREADY_BLOCKED.getMessage());
        }
    }

    private void validateMajorExists(Long majorId) {
        if (!majorRepository.existsById(majorId)) {
            throw new NotFoundException(MemberExceptionMessage.MAJOR_NOT_FOUND.getMessage());
        }
    }

    private void validateInterestExists(Long interestId) {
        if (!interestRepository.existsById(interestId)) {
            throw new NotFoundException(MemberExceptionMessage.INTEREST_NOT_FOUND.getMessage());
        }
    }
}
