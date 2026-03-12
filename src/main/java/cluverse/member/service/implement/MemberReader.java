package cluverse.member.service.implement;

import cluverse.common.exception.NotFoundException;
import cluverse.member.domain.Member;
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
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberReader {

    private final MemberRepository memberRepository;
    private final MemberQueryRepository memberQueryRepository;
    private final FollowRepository followRepository;
    private final BlockRepository blockRepository;
    private final UniversityRepository universityRepository;

    public MemberProfileResponse getProfile(Long viewerId, Long targetMemberId) {
        Member member = readOrThrow(targetMemberId);
        boolean sameMember = viewerId.equals(targetMemberId);
        return MemberProfileResponse.of(
                member,
                member.getProfile(),
                readUniversitySummary(member.getUniversityId()),
                !sameMember && followRepository.existsByFollowerIdAndFollowingId(viewerId, targetMemberId),
                !sameMember && blockRepository.existsByBlockerIdAndBlockedId(viewerId, targetMemberId),
                followRepository.countByFollowingId(targetMemberId),
                followRepository.countByFollowerId(targetMemberId),
                sameMember
        );
    }

    public List<MemberMajorResponse> getMajors(Long memberId) {
        return memberQueryRepository.findMajorsByMemberId(memberId).stream()
                .map(MemberMajorResponse::from)
                .toList();
    }

    public List<MemberInterestResponse> getInterests(Long memberId) {
        return readOrThrow(memberId).getInterests().stream()
                .map(MemberInterestResponse::from)
                .toList();
    }

    public boolean isFollowing(Long followerId, Long followingId) {
        return followRepository.existsByFollowerIdAndFollowingId(followerId, followingId);
    }

    public boolean isBlocked(Long blockerId, Long blockedId) {
        return blockRepository.existsByBlockerIdAndBlockedId(blockerId, blockedId);
    }

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

    public Member readOrThrow(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException(MemberExceptionMessage.MEMBER_NOT_FOUND.getMessage()));
    }

    public MemberProfileSummaryResponse readUniversitySummary(Long universityId) {
        University university = universityRepository.findById(universityId)
                .orElseThrow(() -> new NotFoundException(MemberExceptionMessage.UNIVERSITY_NOT_FOUND.getMessage()));
        return new MemberProfileSummaryResponse(
                university.getId(),
                university.getName(),
                university.getBadgeImageUrl()
        );
    }
}
