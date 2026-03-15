package cluverse.group.service;

import cluverse.common.exception.ForbiddenException;
import cluverse.group.domain.Group;
import cluverse.group.domain.GroupInterest;
import cluverse.group.domain.GroupRole;
import cluverse.group.exception.GroupExceptionMessage;
import cluverse.group.service.implement.GroupReader;
import cluverse.group.service.implement.GroupWriter;
import cluverse.group.service.request.GroupCreateRequest;
import cluverse.group.service.request.GroupMemberUpdateRequest;
import cluverse.group.service.request.GroupOwnerTransferRequest;
import cluverse.group.service.request.GroupRoleCreateRequest;
import cluverse.group.service.request.GroupRoleUpdateRequest;
import cluverse.group.service.request.GroupSearchRequest;
import cluverse.group.service.request.GroupUpdateRequest;
import cluverse.group.service.response.GroupDetailResponse;
import cluverse.group.service.response.GroupInterestResponse;
import cluverse.group.service.response.GroupMemberResponse;
import cluverse.group.service.response.GroupPageResponse;
import cluverse.group.service.response.GroupRoleResponse;
import cluverse.group.service.response.GroupSummaryResponse;
import cluverse.group.service.response.MyGroupSummaryResponse;
import cluverse.interest.domain.Interest;
import cluverse.member.domain.Member;
import cluverse.member.domain.MemberProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class GroupService {

    private final GroupReader groupReader;
    private final GroupWriter groupWriter;

    @Transactional(readOnly = true)
    public GroupPageResponse getGroups(Long memberId, GroupSearchRequest request) {
        List<Group> groups = groupReader.readGroups(request);
        List<GroupSummaryResponse> pageItems = paginate(groups, request.pageOrDefault(), request.sizeOrDefault()).stream()
                .map(this::toSummaryResponse)
                .toList();

        return new GroupPageResponse(
                pageItems,
                request.pageOrDefault(),
                request.sizeOrDefault(),
                groups.size() > request.pageOrDefault() * request.sizeOrDefault()
        );
    }

    @Transactional(readOnly = true)
    public List<MyGroupSummaryResponse> getMyGroups(Long memberId) {
        return groupReader.readMyGroups(memberId).stream()
                .map(group -> MyGroupSummaryResponse.of(
                        group,
                        group.getMember(memberId).getRole(),
                        groupReader.countOpenRecruitments(group.getId())
                ))
                .toList();
    }

    public GroupDetailResponse createGroup(Long memberId, GroupCreateRequest request) {
        Group group = groupWriter.create(memberId, request);
        return toDetailResponse(memberId, groupReader.readOrThrow(group.getId()));
    }

    @Transactional(readOnly = true)
    public GroupDetailResponse getGroup(Long memberId, Long groupId) {
        return toDetailResponse(memberId, groupReader.readOrThrow(groupId));
    }

    public GroupDetailResponse updateGroup(Long memberId, Long groupId, GroupUpdateRequest request) {
        Group group = groupReader.readOrThrow(groupId);
        validateManager(memberId, group);
        groupWriter.update(group, request);
        return toDetailResponse(memberId, group);
    }

    @Transactional(readOnly = true)
    public List<GroupMemberResponse> getMembers(Long memberId, Long groupId) {
        Group group = groupReader.readOrThrow(groupId);
        validateManager(memberId, group);
        return toMemberResponses(memberId, group);
    }

    public GroupMemberResponse updateMember(Long memberId,
                                            Long groupId,
                                            Long targetMemberId,
                                            GroupMemberUpdateRequest request,
                                            String clientIp) {
        Group group = groupReader.readOrThrow(groupId);
        validateManager(memberId, group);
        groupWriter.updateMember(group, targetMemberId, request);
        return toMemberResponses(memberId, group).stream()
                .filter(member -> member.memberId().equals(targetMemberId))
                .findFirst()
                .orElseThrow();
    }

    public void leaveGroup(Long memberId, Long groupId, String clientIp) {
        Group group = groupReader.readOrThrow(groupId);
        groupWriter.leave(group, memberId);
    }

    public void removeMember(Long memberId, Long groupId, Long targetMemberId, String clientIp) {
        Group group = groupReader.readOrThrow(groupId);
        validateManager(memberId, group);
        groupWriter.removeMember(group, targetMemberId);
    }

    public GroupDetailResponse transferOwner(Long memberId,
                                             Long groupId,
                                             GroupOwnerTransferRequest request,
                                             String clientIp) {
        Group group = groupReader.readOrThrow(groupId);
        validateOwner(memberId, group);
        groupWriter.transferOwner(group, request);
        return toDetailResponse(memberId, group);
    }

    @Transactional(readOnly = true)
    public List<GroupRoleResponse> getRoles(Long memberId, Long groupId) {
        Group group = groupReader.readOrThrow(groupId);
        validateManager(memberId, group);
        return toRoleResponses(group);
    }

    public GroupRoleResponse createRole(Long memberId, Long groupId, GroupRoleCreateRequest request) {
        Group group = groupReader.readOrThrow(groupId);
        validateManager(memberId, group);
        GroupRole role = groupWriter.createRole(group, request);
        return toRoleResponse(role);
    }

    public GroupRoleResponse updateRole(Long memberId, Long groupId, Long roleId, GroupRoleUpdateRequest request) {
        Group group = groupReader.readOrThrow(groupId);
        validateManager(memberId, group);
        GroupRole role = groupWriter.updateRole(group, roleId, request);
        return toRoleResponse(role);
    }

    public void deleteRole(Long memberId, Long groupId, Long roleId) {
        Group group = groupReader.readOrThrow(groupId);
        validateManager(memberId, group);
        groupWriter.deleteRole(group, roleId);
    }

    private GroupSummaryResponse toSummaryResponse(Group group) {
        long openRecruitmentCount = groupReader.countOpenRecruitments(group.getId());
        return GroupSummaryResponse.of(
                group,
                openRecruitmentCount > 0,
                openRecruitmentCount,
                toInterestResponses(group)
        );
    }

    private GroupDetailResponse toDetailResponse(Long memberId, Group group) {
        Map<Long, Member> memberMap = groupReader.readMemberMap(List.of(group.getOwnerId()));
        Member owner = memberMap.get(group.getOwnerId());

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
                owner == null ? null : owner.getNickname(),
                group.getMaxMembers(),
                group.getMemberCount(),
                memberId != null && group.hasMember(memberId),
                memberId != null && group.hasMember(memberId) ? group.getMember(memberId).getRole() : null,
                groupReader.countOpenRecruitments(group.getId()),
                toInterestResponses(group),
                toRoleResponses(group),
                group.getCreatedAt(),
                group.getUpdatedAt()
        );
    }

    private List<GroupMemberResponse> toMemberResponses(Long viewerId, Group group) {
        List<Long> memberIds = group.getMembers().stream()
                .map(member -> member.getMemberId())
                .toList();
        Map<Long, Member> memberMap = groupReader.readMemberMap(memberIds);

        return group.getMembers().stream()
                .map(member -> {
                    Member foundMember = memberMap.get(member.getMemberId());
                    MemberProfile profile = foundMember == null ? null : foundMember.getProfile();
                    GroupRole customTitle = group.getRoles().stream()
                            .filter(role -> role.getId().equals(member.getCustomTitleId()))
                            .findFirst()
                            .orElse(null);
                    return new GroupMemberResponse(
                            member.getMemberId(),
                            foundMember == null ? null : foundMember.getNickname(),
                            profile == null ? null : profile.getProfileImageUrl(),
                            member.getRole(),
                            member.getCustomTitleId(),
                            customTitle == null ? null : customTitle.getTitle(),
                            member.getJoinedAt(),
                            viewerId != null && viewerId.equals(member.getMemberId())
                    );
                })
                .toList();
    }

    private List<GroupInterestResponse> toInterestResponses(Group group) {
        Map<Long, Interest> interestMap = groupReader.readInterestMap(
                group.getInterests().stream().map(GroupInterest::getInterestId).toList()
        );
        return group.getInterests().stream()
                .map(interest -> GroupInterestResponse.of(
                        interest,
                        interestMap.containsKey(interest.getInterestId())
                                ? interestMap.get(interest.getInterestId()).getName()
                                : null
                ))
                .toList();
    }

    private List<GroupRoleResponse> toRoleResponses(Group group) {
        return group.getRoles().stream()
                .map(this::toRoleResponse)
                .toList();
    }

    private GroupRoleResponse toRoleResponse(GroupRole role) {
        return GroupRoleResponse.from(role);
    }

    private List<Group> paginate(List<Group> items, int page, int size) {
        int fromIndex = Math.min((page - 1) * size, items.size());
        int toIndex = Math.min(fromIndex + size, items.size());
        return items.subList(fromIndex, toIndex);
    }

    private void validateManager(Long memberId, Group group) {
        if (!group.isManager(memberId)) {
            throw new ForbiddenException(GroupExceptionMessage.GROUP_ACCESS_DENIED.getMessage());
        }
    }

    private void validateOwner(Long memberId, Group group) {
        if (!group.isOwner(memberId)) {
            throw new ForbiddenException(GroupExceptionMessage.GROUP_ACCESS_DENIED.getMessage());
        }
    }
}
