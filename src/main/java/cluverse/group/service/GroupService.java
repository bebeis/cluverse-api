package cluverse.group.service;

import cluverse.board.domain.Board;
import cluverse.board.service.implement.BoardReader;
import cluverse.board.service.implement.BoardWriter;
import cluverse.common.exception.ForbiddenException;
import cluverse.group.domain.Group;
import cluverse.group.domain.GroupInterest;
import cluverse.group.domain.GroupRole;
import cluverse.group.exception.GroupExceptionMessage;
import cluverse.group.repository.dto.GroupMemberSummaryQueryDto;
import cluverse.group.service.implement.GroupReader;
import cluverse.group.service.implement.GroupWriter;
import cluverse.group.service.request.GroupCreateRequest;
import cluverse.group.service.request.GroupMemberUpdateRequest;
import cluverse.group.service.request.GroupOwnerTransferRequest;
import cluverse.group.service.request.GroupRoleCreateRequest;
import cluverse.group.service.request.GroupRoleUpdateRequest;
import cluverse.group.service.request.GroupUpdateRequest;
import cluverse.group.service.response.GroupDetailResponse;
import cluverse.group.service.response.GroupInterestResponse;
import cluverse.group.service.response.GroupMemberResponse;
import cluverse.group.service.response.GroupRoleResponse;
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
    private final BoardReader boardReader;
    private final BoardWriter boardWriter;

    public GroupDetailResponse createGroup(Long memberId, GroupCreateRequest request) {
        Board board = boardWriter.createGroupBoard(request.name(), request.description());
        Group group = groupWriter.create(memberId, board.getId(), request);
        return toDetailResponse(memberId, groupReader.readActiveOrThrow(group.getId()));
    }

    public GroupDetailResponse updateGroup(Long memberId, Long groupId, GroupUpdateRequest request) {
        Group group = groupReader.readActiveOrThrow(groupId);
        validateManager(memberId, group);
        groupWriter.update(group, request);
        Board board = boardReader.readOrThrow(group.getBoardId());
        boardWriter.updateGroupBoard(board, request.name(), request.description());
        return toDetailResponse(memberId, group);
    }

    public GroupMemberResponse updateMember(Long memberId,
                                            Long groupId,
                                            Long targetMemberId,
                                            GroupMemberUpdateRequest request,
                                            String clientIp) {
        Group group = groupReader.readActiveOrThrow(groupId);
        validateManager(memberId, group);
        groupWriter.updateMember(group, targetMemberId, request);
        return toMemberResponses(memberId, group).stream()
                .filter(member -> member.memberId().equals(targetMemberId))
                .findFirst()
                .orElseThrow();
    }

    public void leaveGroup(Long memberId, Long groupId, String clientIp) {
        Group group = groupReader.readActiveOrThrow(groupId);
        groupWriter.leave(group, memberId);
    }

    public void removeMember(Long memberId, Long groupId, Long targetMemberId, String clientIp) {
        Group group = groupReader.readActiveOrThrow(groupId);
        validateManager(memberId, group);
        groupWriter.removeMember(group, targetMemberId);
    }

    public GroupDetailResponse transferOwner(Long memberId,
                                             Long groupId,
                                             GroupOwnerTransferRequest request,
                                             String clientIp) {
        Group group = groupReader.readActiveOrThrow(groupId);
        validateOwner(memberId, group);
        groupWriter.transferOwner(group, request);
        return toDetailResponse(memberId, group);
    }

    public GroupRoleResponse createRole(Long memberId, Long groupId, GroupRoleCreateRequest request) {
        Group group = groupReader.readActiveOrThrow(groupId);
        validateManager(memberId, group);
        GroupRole role = groupWriter.createRole(group, request);
        return GroupRoleResponse.from(role);
    }

    public GroupRoleResponse updateRole(Long memberId, Long groupId, Long roleId, GroupRoleUpdateRequest request) {
        Group group = groupReader.readActiveOrThrow(groupId);
        validateManager(memberId, group);
        GroupRole role = groupWriter.updateRole(group, roleId, request);
        return GroupRoleResponse.from(role);
    }

    public void deleteRole(Long memberId, Long groupId, Long roleId) {
        Group group = groupReader.readActiveOrThrow(groupId);
        validateManager(memberId, group);
        groupWriter.deleteRole(group, roleId);
    }

    public void deleteGroup(Long memberId, Long groupId) {
        Group group = groupReader.readActiveOrThrow(groupId);
        validateOwner(memberId, group);
        groupWriter.close(group);
        Board board = boardReader.readOrThrow(group.getBoardId());
        boardWriter.deactivateGroupBoard(board);
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
                .map(interest -> GroupInterestResponse.of(interest, interestNameMap.get(interest.getInterestId())))
                .toList();
    }

    private List<GroupRoleResponse> toRoleResponses(Group group) {
        return group.getRoles().stream()
                .map(GroupRoleResponse::from)
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
            throw new ForbiddenException(GroupExceptionMessage.GROUP_ACCESS_DENIED.getMessage());
        }
    }

    private void validateOwner(Long memberId, Group group) {
        if (!group.isOwner(memberId)) {
            throw new ForbiddenException(GroupExceptionMessage.GROUP_ACCESS_DENIED.getMessage());
        }
    }
}
