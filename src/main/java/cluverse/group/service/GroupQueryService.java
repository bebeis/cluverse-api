package cluverse.group.service;

import cluverse.group.domain.Group;
import cluverse.group.domain.GroupInterest;
import cluverse.group.domain.GroupRole;
import cluverse.group.repository.dto.GroupMemberSummaryQueryDto;
import cluverse.group.service.implement.GroupReader;
import cluverse.group.service.request.GroupSearchRequest;
import cluverse.group.service.response.GroupDetailResponse;
import cluverse.group.service.response.GroupInterestResponse;
import cluverse.group.service.response.GroupMemberResponse;
import cluverse.group.service.response.GroupPageResponse;
import cluverse.group.service.response.GroupRoleResponse;
import cluverse.group.service.response.GroupSummaryResponse;
import cluverse.group.service.response.MyGroupSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GroupQueryService {

    private final GroupReader groupReader;

    public GroupPageResponse getGroups(Long memberId, GroupSearchRequest request) {
        List<Group> groups = groupReader.readGroups(request);
        Map<Long, Long> openRecruitmentCountMap = groupReader.countOpenRecruitments(
                groups.stream().map(Group::getId).toList()
        );
        List<Group> filteredGroups = filterRecruitableGroups(groups, request, openRecruitmentCountMap);
        List<Group> pagedGroups = paginate(filteredGroups, request.pageOrDefault(), request.sizeOrDefault());
        Map<Long, String> interestNameMap = groupReader.readInterestNameMap(extractInterestIds(pagedGroups));
        List<GroupSummaryResponse> pageItems = pagedGroups.stream()
                .map(group -> toSummaryResponse(group, openRecruitmentCountMap, interestNameMap))
                .toList();

        return new GroupPageResponse(
                pageItems,
                request.pageOrDefault(),
                request.sizeOrDefault(),
                filteredGroups.size() > request.pageOrDefault() * request.sizeOrDefault()
        );
    }

    public List<MyGroupSummaryResponse> getMyGroups(Long memberId) {
        List<Group> groups = groupReader.readMyGroups(memberId);
        Map<Long, Long> openRecruitmentCountMap = groupReader.countOpenRecruitments(
                groups.stream().map(Group::getId).toList()
        );
        return groups.stream()
                .map(group -> MyGroupSummaryResponse.of(
                        group,
                        group.getMember(memberId).getRole(),
                        openRecruitmentCountMap.getOrDefault(group.getId(), 0L)
                ))
                .toList();
    }

    public Group readGroupOrThrow(Long groupId) {
        return groupReader.readOrThrow(groupId);
    }

    public GroupDetailResponse getGroup(Long memberId, Long groupId) {
        return toDetailResponse(memberId, groupReader.readActiveOrThrow(groupId));
    }

    public List<GroupMemberResponse> getMembers(Long memberId, Long groupId) {
        Group group = groupReader.readActiveOrThrow(groupId);
        validateManager(memberId, group);
        return toMemberResponses(memberId, group);
    }

    public List<GroupRoleResponse> getRoles(Long memberId, Long groupId) {
        Group group = groupReader.readActiveOrThrow(groupId);
        validateManager(memberId, group);
        return toRoleResponses(group);
    }

    private GroupSummaryResponse toSummaryResponse(Group group,
                                                   Map<Long, Long> openRecruitmentCountMap,
                                                   Map<Long, String> interestNameMap) {
        long openRecruitmentCount = openRecruitmentCountMap.getOrDefault(group.getId(), 0L);
        return GroupSummaryResponse.of(
                group,
                openRecruitmentCount > 0,
                openRecruitmentCount,
                toInterestResponses(group, interestNameMap)
        );
    }

    private GroupDetailResponse toDetailResponse(Long memberId, Group group) {
        Map<Long, GroupMemberSummaryQueryDto> memberMap = groupReader.readMemberSummaryMap(List.of(group.getOwnerId()));
        GroupMemberSummaryQueryDto owner = memberMap.get(group.getOwnerId());
        Map<Long, Long> openRecruitmentCountMap = groupReader.countOpenRecruitments(List.of(group.getId()));
        Map<Long, String> interestNameMap = groupReader.readInterestNameMap(extractInterestIds(List.of(group)));

        return new GroupDetailResponse(
                group.getId(),
                group.getBoardId(),
                group.getName(),
                group.getDescription(),
                group.getCoverImageUrl(),
                group.getCategory(),
                group.getActivityType(),
                group.getRegion(),
                group.getVisibility(),
                group.getStatus(),
                group.getOwnerId(),
                owner == null ? null : owner.nickname(),
                group.getMaxMembers(),
                group.getMemberCount(),
                memberId != null && group.hasMember(memberId),
                memberId != null && group.hasMember(memberId) ? group.getMember(memberId).getRole() : null,
                openRecruitmentCountMap.getOrDefault(group.getId(), 0L),
                toInterestResponses(group, interestNameMap),
                toRoleResponses(group),
                group.getCreatedAt(),
                group.getUpdatedAt()
        );
    }

    private List<GroupMemberResponse> toMemberResponses(Long viewerId, Group group) {
        List<Long> memberIds = group.getMembers().stream()
                .map(member -> member.getMemberId())
                .toList();
        Map<Long, GroupMemberSummaryQueryDto> memberMap = groupReader.readMemberSummaryMap(memberIds);

        return group.getMembers().stream()
                .map(member -> {
                    GroupMemberSummaryQueryDto foundMember = memberMap.get(member.getMemberId());
                    GroupRole customTitle = group.getRoles().stream()
                            .filter(role -> role.getId().equals(member.getCustomTitleId()))
                            .findFirst()
                            .orElse(null);
                    return new GroupMemberResponse(
                            member.getMemberId(),
                            foundMember == null ? null : foundMember.nickname(),
                            foundMember == null ? null : foundMember.profileImageUrl(),
                            member.getRole(),
                            member.getCustomTitleId(),
                            customTitle == null ? null : customTitle.getTitle(),
                            member.getJoinedAt(),
                            viewerId != null && viewerId.equals(member.getMemberId())
                    );
                })
                .toList();
    }

    private List<GroupInterestResponse> toInterestResponses(Group group, Map<Long, String> interestNameMap) {
        return group.getInterests().stream()
                .map(interest -> GroupInterestResponse.of(
                        interest,
                        interestNameMap.get(interest.getInterestId())
                ))
                .toList();
    }

    private List<GroupRoleResponse> toRoleResponses(Group group) {
        return group.getRoles().stream()
                .map(GroupRoleResponse::from)
                .toList();
    }

    private List<Group> paginate(List<Group> items, int page, int size) {
        int fromIndex = Math.min((page - 1) * size, items.size());
        int toIndex = Math.min(fromIndex + size, items.size());
        return items.subList(fromIndex, toIndex);
    }

    private List<Group> filterRecruitableGroups(List<Group> groups,
                                                GroupSearchRequest request,
                                                Map<Long, Long> openRecruitmentCountMap) {
        if (!Boolean.TRUE.equals(request.recruitableOnly())) {
            return groups;
        }
        return groups.stream()
                .filter(group -> openRecruitmentCountMap.getOrDefault(group.getId(), 0L) > 0)
                .toList();
    }

    private List<Long> extractInterestIds(List<Group> groups) {
        return groups.stream()
                .flatMap(group -> group.getInterests().stream())
                .map(GroupInterest::getInterestId)
                .distinct()
                .toList();
    }

    private void validateManager(Long memberId, Group group) {
        if (!group.isManager(memberId)) {
            throw new cluverse.common.exception.ForbiddenException(cluverse.group.exception.GroupExceptionMessage.GROUP_ACCESS_DENIED.getMessage());
        }
    }
}
