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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class MemberWriter {

    private final FollowRepository followRepository;
    private final BlockRepository blockRepository;
    private final MajorRepository majorRepository;
    private final InterestRepository interestRepository;

    public void updateProfile(Member member, UpdateProfileRequest request) {
        MemberProfile profile = ensureProfile(member);
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
    }

    public MemberMajor addMajor(Member member, AddMajorRequest request) {
        validateMajorExists(request.majorId());
        member.addMajor(request.majorId(), request.majorType());
        return member.getMajors().stream()
                .filter(major -> major.getMajorId().equals(request.majorId()))
                .findFirst()
                .orElseThrow();
    }

    public void removeMajor(Member member, Long majorId) {
        validateMajorExists(majorId);
        member.removeMajor(majorId);
    }

    public void addInterest(Member member, AddInterestRequest request) {
        validateInterestExists(request.interestId());
        member.addInterest(request.interestId());
    }

    public void removeInterest(Member member, Long interestId) {
        validateInterestExists(interestId);
        member.removeInterest(interestId);
    }

    public void follow(Long followerId, Long followingId) {
        validateCannotTargetSelf(followerId, followingId, MemberExceptionMessage.CANNOT_FOLLOW_SELF);
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

    private MemberProfile ensureProfile(Member member) {
        MemberProfile profile = member.getProfile();
        if (profile == null) {
            profile = MemberProfile.create(member);
            member.initProfile(profile);
        }
        return profile;
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
