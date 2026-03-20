package cluverse.member.service.implement;

import cluverse.common.exception.NotFoundException;
import cluverse.member.domain.Member;
import cluverse.member.exception.MemberExceptionMessage;
import cluverse.member.repository.BlockRepository;
import cluverse.member.repository.FollowRepository;
import cluverse.member.repository.MemberQueryRepository;
import cluverse.member.repository.MemberRepository;
import cluverse.member.service.response.BlockedMemberResponse;
import cluverse.member.service.response.MemberFollowResponse;
import cluverse.member.service.response.MemberInterestResponse;
import cluverse.member.service.response.MemberMajorResponse;
import cluverse.member.service.response.MemberProfileSummaryResponse;
import cluverse.university.domain.University;
import cluverse.university.repository.UniversityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberReader {

    private final MemberRepository memberRepository;
    private final MemberQueryRepository memberQueryRepository;
    private final FollowRepository followRepository;
    private final BlockRepository blockRepository;
    private final UniversityRepository universityRepository;

    public List<MemberMajorResponse> readMajors(Long memberId) {
        return memberQueryRepository.findMajorDetailsByMemberId(memberId).stream()
                .map(result -> new MemberMajorResponse(
                        result.memberMajorId(),
                        result.majorId(),
                        result.majorType(),
                        result.majorName(),
                        result.collegeName()
                ))
                .toList();
    }

    public MemberMajorResponse readMajor(Long memberMajorId) {
        MemberQueryRepository.MemberMajorDetailDto result = memberQueryRepository.findMajorDetailByMemberMajorId(memberMajorId);
        if (result == null) {
            throw new NotFoundException(MemberExceptionMessage.MAJOR_NOT_FOUND.getMessage());
        }
        return new MemberMajorResponse(
                result.memberMajorId(),
                result.majorId(),
                result.majorType(),
                result.majorName(),
                result.collegeName()
        );
    }

    public List<MemberInterestResponse> readInterests(Long memberId) {
        List<Long> interestIds = readOrThrow(memberId).getInterests();
        Map<Long, MemberQueryRepository.MemberInterestDetailDto> detailByInterestId =
                memberQueryRepository.findInterestDetailsByInterestIds(interestIds).stream()
                        .collect(java.util.stream.Collectors.toMap(
                                MemberQueryRepository.MemberInterestDetailDto::interestId,
                                detail -> detail,
                                (left, right) -> left,
                                LinkedHashMap::new
                        ));

        return interestIds.stream()
                .map(detailByInterestId::get)
                .filter(java.util.Objects::nonNull)
                .map(detail -> new MemberInterestResponse(
                        detail.interestId(),
                        detail.interestName(),
                        detail.category()
                ))
                .toList();
    }

    public MemberInterestResponse readInterest(Long interestId) {
        MemberQueryRepository.MemberInterestDetailDto detail = memberQueryRepository.findInterestDetailByInterestId(interestId);
        if (detail == null) {
            throw new NotFoundException(MemberExceptionMessage.INTEREST_NOT_FOUND.getMessage());
        }
        return new MemberInterestResponse(
                detail.interestId(),
                detail.interestName(),
                detail.category()
        );
    }

    public boolean isFollowing(Long followerId, Long followingId) {
        return followRepository.existsByFollowerIdAndFollowingId(followerId, followingId);
    }

    public boolean isBlocked(Long blockerId, Long blockedId) {
        return blockRepository.existsByBlockerIdAndBlockedId(blockerId, blockedId);
    }

    public long countFollowers(Long memberId) {
        return followRepository.countByFollowingId(memberId);
    }

    public long countFollowings(Long memberId) {
        return followRepository.countByFollowerId(memberId);
    }

    public long countPosts(Long memberId) {
        return memberQueryRepository.countActivePostsByMemberId(memberId);
    }

    public List<BlockedMemberResponse> readBlockedMembers(Long blockerId) {
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

    public List<MemberFollowResponse> readFollowers(Long memberId) {
        return memberQueryRepository.findFollowers(memberId).stream()
                .map(result -> new MemberFollowResponse(
                        result.memberId(),
                        result.nickname(),
                        result.profileImageUrl()
                ))
                .toList();
    }

    public List<MemberFollowResponse> readFollowings(Long memberId) {
        return memberQueryRepository.findFollowings(memberId).stream()
                .map(result -> new MemberFollowResponse(
                        result.memberId(),
                        result.nickname(),
                        result.profileImageUrl()
                ))
                .toList();
    }

    public Member readOrThrow(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException(MemberExceptionMessage.MEMBER_NOT_FOUND.getMessage()));
    }

    public Map<Long, Member> readMemberMap(Collection<Long> memberIds) {
        if (memberIds == null || memberIds.isEmpty()) {
            return Map.of();
        }
        return memberRepository.findAllById(memberIds).stream()
                .collect(java.util.stream.Collectors.toMap(Member::getId, member -> member));
    }

    public MemberProfileSummaryResponse readUniversitySummary(Long universityId) {
        if (universityId == null) {
            return null;
        }
        University university = universityRepository.findById(universityId)
                .orElseThrow(() -> new NotFoundException(MemberExceptionMessage.UNIVERSITY_NOT_FOUND.getMessage()));
        return new MemberProfileSummaryResponse(
                university.getId(),
                university.getName(),
                university.getBadgeImageUrl()
        );
    }
}
