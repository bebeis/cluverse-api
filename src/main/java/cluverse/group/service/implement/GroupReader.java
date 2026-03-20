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

    public List<Group> readGroups(GroupSearchRequest request) {
        return groupRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .filter(group -> group.getStatus() == GroupStatus.ACTIVE)
                .filter(group -> request.keyword() == null
                        || group.getName().toLowerCase().contains(request.keyword().toLowerCase()))
                .filter(group -> request.category() == null || group.getCategory() == request.category())
                .filter(group -> request.activityType() == null || group.getActivityType() == request.activityType())
                .filter(group -> request.region() == null || request.region().equalsIgnoreCase(group.getRegion()))
                .filter(group -> request.visibility() == null || group.getVisibility() == request.visibility())
                .toList();
    }

    public List<Group> readMyGroups(Long memberId) {
        return groupRepository.findAllByMemberId(memberId).stream()
                .filter(group -> group.getStatus() == GroupStatus.ACTIVE)
                .toList();
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
