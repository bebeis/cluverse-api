package cluverse.group.service.implement;

import cluverse.common.exception.NotFoundException;
import cluverse.group.domain.Group;
import cluverse.group.exception.GroupExceptionMessage;
import cluverse.group.repository.GroupRepository;
import cluverse.group.service.request.GroupSearchRequest;
import cluverse.interest.domain.Interest;
import cluverse.interest.repository.InterestRepository;
import cluverse.member.domain.Member;
import cluverse.member.repository.MemberRepository;
import cluverse.recruitment.domain.RecruitmentStatus;
import cluverse.recruitment.repository.RecruitmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GroupReader {

    private final GroupRepository groupRepository;
    private final RecruitmentRepository recruitmentRepository;
    private final MemberRepository memberRepository;
    private final InterestRepository interestRepository;

    public Group readOrThrow(Long groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException(GroupExceptionMessage.GROUP_NOT_FOUND.getMessage()));
    }

    public List<Group> readGroups(GroupSearchRequest request) {
        return groupRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .filter(group -> request.keyword() == null
                        || group.getName().toLowerCase().contains(request.keyword().toLowerCase()))
                .filter(group -> request.category() == null || group.getCategory() == request.category())
                .filter(group -> request.activityType() == null || group.getActivityType() == request.activityType())
                .filter(group -> request.region() == null || request.region().equalsIgnoreCase(group.getRegion()))
                .filter(group -> request.visibility() == null || group.getVisibility() == request.visibility())
                .filter(group -> !Boolean.TRUE.equals(request.recruitableOnly()) || countOpenRecruitments(group.getId()) > 0)
                .toList();
    }

    public List<Group> readMyGroups(Long memberId) {
        return groupRepository.findAllByMemberId(memberId);
    }

    public long countOpenRecruitments(Long groupId) {
        return recruitmentRepository.countByGroupIdAndStatusAndDeletedAtIsNull(groupId, RecruitmentStatus.OPEN);
    }

    public Map<Long, Long> countOpenRecruitments(Collection<Long> groupIds) {
        return groupIds.stream()
                .distinct()
                .collect(Collectors.toMap(Function.identity(), this::countOpenRecruitments));
    }

    public Map<Long, Member> readMemberMap(Collection<Long> memberIds) {
        return memberRepository.findAllById(memberIds).stream()
                .collect(Collectors.toMap(Member::getId, Function.identity()));
    }

    public Map<Long, Interest> readInterestMap(Collection<Long> interestIds) {
        return interestRepository.findAllById(interestIds).stream()
                .collect(Collectors.toMap(Interest::getId, Function.identity()));
    }
}
