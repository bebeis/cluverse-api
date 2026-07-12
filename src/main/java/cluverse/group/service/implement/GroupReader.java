package cluverse.group.service.implement;

import cluverse.common.exception.NotFoundException;
import cluverse.group.domain.Group;
import cluverse.group.domain.GroupStatus;
import cluverse.group.exception.GroupExceptionMessage;
import cluverse.group.repository.GroupQueryRepository;
import cluverse.group.repository.GroupRepository;
import cluverse.group.repository.dto.GroupMemberSummaryQueryDto;
import cluverse.group.service.request.GroupSearchRequest;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
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
    private final GroupQueryRepository groupQueryRepository;

    public Group readOrThrow(Long groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException(GroupExceptionMessage.GROUP_NOT_FOUND.getMessage()));
    }

    public Group readActiveOrThrow(Long groupId) {
        return groupRepository.findById(groupId)
                .filter(group -> group.getStatus() == GroupStatus.ACTIVE)
                .orElseThrow(() -> new NotFoundException(GroupExceptionMessage.GROUP_NOT_FOUND.getMessage()));
    }

    public Group readWithMembersOrThrow(Long groupId) {
        return groupRepository.findWithMembersById(groupId)
                .orElseThrow(() -> new NotFoundException(GroupExceptionMessage.GROUP_NOT_FOUND.getMessage()));
    }

    public Group readActiveDetailOrThrow(Long groupId) {
        Group group = groupRepository.findById(groupId)
                .filter(target -> target.getStatus() == GroupStatus.ACTIVE)
                .orElseThrow(() -> new NotFoundException(GroupExceptionMessage.GROUP_NOT_FOUND.getMessage()));
        Hibernate.initialize(group.getMembers());
        Hibernate.initialize(group.getRoles());
        Hibernate.initialize(group.getInterests());
        return group;
    }

    public Group readActiveWithMembersAndRolesOrThrow(Long groupId) {
        Group group = groupRepository.findById(groupId)
                .filter(target -> target.getStatus() == GroupStatus.ACTIVE)
                .orElseThrow(() -> new NotFoundException(GroupExceptionMessage.GROUP_NOT_FOUND.getMessage()));
        Hibernate.initialize(group.getMembers());
        Hibernate.initialize(group.getRoles());
        return group;
    }

    public List<Group> readGroups(GroupSearchRequest request) {
        List<Group> groups = groupRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .filter(group -> group.getStatus() == GroupStatus.ACTIVE)
                .filter(group -> request.keyword() == null
                        || group.getName().toLowerCase().contains(request.keyword().toLowerCase()))
                .filter(group -> request.category() == null || group.getCategory() == request.category())
                .filter(group -> request.activityType() == null || group.getActivityType() == request.activityType())
                .filter(group -> request.region() == null || request.region().equalsIgnoreCase(group.getRegion()))
                .filter(group -> request.visibility() == null || group.getVisibility() == request.visibility())
                .toList();
        groups.forEach(group -> Hibernate.initialize(group.getInterests()));
        return groups;
    }

    public List<Group> readMyGroups(Long memberId) {
        List<Group> groups = groupRepository.findAllByMemberId(memberId).stream()
                .filter(group -> group.getStatus() == GroupStatus.ACTIVE)
                .toList();
        groups.forEach(group -> Hibernate.initialize(group.getMembers()));
        return groups;
    }

    public long countOpenRecruitments(Long groupId) {
        return countOpenRecruitments(List.of(groupId)).getOrDefault(groupId, 0L);
    }

    public Map<Long, Long> countOpenRecruitments(Collection<Long> groupIds) {
        if (groupIds == null || groupIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, Long> countMap = groupQueryRepository.countOpenRecruitments(groupIds);
        return groupIds.stream()
                .distinct()
                .collect(Collectors.toMap(Function.identity(), groupId -> countMap.getOrDefault(groupId, 0L)));
    }

    public Map<Long, GroupMemberSummaryQueryDto> readMemberSummaryMap(Collection<Long> memberIds) {
        return groupQueryRepository.readMemberSummaryMap(memberIds);
    }

    public Map<Long, String> readInterestNameMap(Collection<Long> interestIds) {
        return groupQueryRepository.readInterestNameMap(interestIds);
    }
}
