package cluverse.member.service;

import cluverse.common.exception.BadRequestException;
import cluverse.common.exception.NotFoundException;
import cluverse.interest.repository.InterestRepository;
import cluverse.major.repository.MajorRepository;
import cluverse.member.service.request.AddInterestRequest;
import cluverse.member.service.request.AddMajorRequest;
import cluverse.member.service.request.UpdateProfileRequest;
import cluverse.member.domain.*;
import cluverse.member.exception.MemberExceptionMessage;
import cluverse.member.repository.BlockRepository;
import cluverse.member.repository.FollowRepository;
import cluverse.member.repository.MemberQueryRepository;
import cluverse.member.repository.MemberRepository;
import cluverse.member.service.response.BlockedMemberResponse;
import cluverse.member.service.response.MemberInterestResponse;
import cluverse.member.service.response.MemberMajorResponse;
import cluverse.member.service.response.MemberProfileResponse;
import cluverse.member.service.response.MemberProfileSummaryResponse;
import cluverse.university.domain.University;
import cluverse.university.repository.UniversityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberService {

    private final MemberRepository memberRepository;
    private final MemberQueryRepository memberQueryRepository;
    private final FollowRepository followRepository;
    private final BlockRepository blockRepository;
    private final MajorRepository majorRepository;
    private final InterestRepository interestRepository;
    private final UniversityRepository universityRepository;

    @Transactional(readOnly = true)
    public MemberProfileResponse getProfile(Long viewerId, Long targetMemberId) {
        Member member = findMemberOrThrow(targetMemberId);
        boolean sameMember = viewerId.equals(targetMemberId);
        return MemberProfileResponse.of(
                member,
                member.getProfile(),
                getUniversitySummary(member.getUniversityId()),
                !sameMember && followRepository.existsByFollowerIdAndFollowingId(viewerId, targetMemberId),
                !sameMember && blockRepository.existsByBlockerIdAndBlockedId(viewerId, targetMemberId),
                followRepository.countByFollowingId(targetMemberId),
                followRepository.countByFollowerId(targetMemberId),
                sameMember
        );
    }

    public MemberProfileResponse updateProfile(Long memberId, UpdateProfileRequest request) {
        Member member = findMemberOrThrow(memberId);
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
                getUniversitySummary(member.getUniversityId()),
                false,
                false,
                followRepository.countByFollowingId(memberId),
                followRepository.countByFollowerId(memberId),
                true
        );
    }

    @Transactional(readOnly = true)
    public List<MemberMajorResponse> getMajors(Long memberId) {
        return memberQueryRepository.findMajorsByMemberId(memberId).stream()
                .map(MemberMajorResponse::from)
                .toList();
    }

    public MemberMajorResponse addMajor(Long memberId, AddMajorRequest request) {
        validateMajorExists(request.majorId());
        Member member = findMemberOrThrow(memberId);
        member.addMajor(request.majorId(), request.majorType());
        MemberMajor added = member.getMajors().stream()
                .filter(m -> m.getMajorId().equals(request.majorId()))
                .findFirst()
                .orElseThrow();
        return MemberMajorResponse.from(added);
    }

    public void removeMajor(Long memberId, Long majorId) {
        validateMajorExists(majorId);
        Member member = findMemberOrThrow(memberId);
        member.removeMajor(majorId);
    }

    @Transactional(readOnly = true)
    public List<MemberInterestResponse> getInterests(Long memberId) {
        return memberQueryRepository.findInterestsByMemberId(memberId).stream()
                .map(MemberInterestResponse::from)
                .toList();
    }

    public MemberInterestResponse addInterest(Long memberId, AddInterestRequest request) {
        validateInterestExists(request.interestId());
        Member member = findMemberOrThrow(memberId);
        member.addInterest(request.interestId());
        MemberInterest added = member.getInterests().stream()
                .filter(i -> i.getInterestId().equals(request.interestId()))
                .findFirst()
                .orElseThrow();
        return MemberInterestResponse.from(added);
    }

    public void removeInterest(Long memberId, Long interestId) {
        validateInterestExists(interestId);
        Member member = findMemberOrThrow(memberId);
        member.removeInterest(interestId);
    }

    public void follow(Long followerId, Long followingId) {
        validateCannotTargetSelf(followerId, followingId, MemberExceptionMessage.CANNOT_FOLLOW_SELF);
        validateMemberExists(followingId);
        validateNotAlreadyFollowing(followerId, followingId);
        followRepository.save(Follow.of(followerId, followingId));
    }

    public void unfollow(Long followerId, Long followingId) {
        Follow follow = followRepository.findByFollowerIdAndFollowingId(followerId, followingId)
                .orElseThrow(() -> new BadRequestException(MemberExceptionMessage.NOT_FOLLOWING.getMessage()));
        followRepository.delete(follow);
    }

    @Transactional(readOnly = true)
    public boolean isFollowing(Long followerId, Long followingId) {
        return followRepository.existsByFollowerIdAndFollowingId(followerId, followingId);
    }

    public void block(Long blockerId, Long blockedId) {
        validateCannotTargetSelf(blockerId, blockedId, MemberExceptionMessage.CANNOT_BLOCK_SELF);
        validateMemberExists(blockedId);
        validateNotAlreadyBlocked(blockerId, blockedId);
        blockRepository.save(Block.of(blockerId, blockedId));
    }

    public void unblock(Long blockerId, Long blockedId) {
        Block block = blockRepository.findByBlockerIdAndBlockedId(blockerId, blockedId)
                .orElseThrow(() -> new BadRequestException(MemberExceptionMessage.NOT_BLOCKED.getMessage()));
        blockRepository.delete(block);
    }

    @Transactional(readOnly = true)
    public boolean isBlocked(Long blockerId, Long blockedId) {
        return blockRepository.existsByBlockerIdAndBlockedId(blockerId, blockedId);
    }

    @Transactional(readOnly = true)
    public List<BlockedMemberResponse> getBlockedMembers(Long blockerId) {
        return memberQueryRepository.findBlockedMembersByBlockerId(blockerId).stream()
                .map(result -> new BlockedMemberResponse(
                        result.memberId(),
                        result.nickname(),
                        result.universityId(),
                        result.universityName(),
                        result.universityBadgeImageUrl(),
                        result.profileImageUrl(),
                        result.blockedAt()
                ))
                .toList();
    }

    private Member findMemberOrThrow(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException(MemberExceptionMessage.MEMBER_NOT_FOUND.getMessage()));
    }

    private void validateMemberExists(Long memberId) {
        if (!memberRepository.existsById(memberId)) {
            throw new NotFoundException(MemberExceptionMessage.MEMBER_NOT_FOUND.getMessage());
        }
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

    private MemberProfileSummaryResponse getUniversitySummary(Long universityId) {
        University university = universityRepository.findById(universityId)
                .orElseThrow(() -> new NotFoundException(MemberExceptionMessage.UNIVERSITY_NOT_FOUND.getMessage()));
        return new MemberProfileSummaryResponse(
                university.getId(),
                university.getName(),
                university.getBadgeImageUrl()
        );
    }
}
