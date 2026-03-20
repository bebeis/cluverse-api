package cluverse.group.service.implement;

import cluverse.group.domain.Group;
import cluverse.group.domain.GroupRole;
import cluverse.group.repository.GroupRepository;
import cluverse.group.service.request.GroupCreateRequest;
import cluverse.group.service.request.GroupMemberUpdateRequest;
import cluverse.group.service.request.GroupOwnerTransferRequest;
import cluverse.group.service.request.GroupRoleCreateRequest;
import cluverse.group.service.request.GroupRoleUpdateRequest;
import cluverse.group.service.request.GroupUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class GroupWriter {

    private static final String DEFAULT_MANAGER_ROLE_TITLE = "운영진";
    private static final String DEFAULT_MEMBER_ROLE_TITLE = "멤버";
    private static final int DEFAULT_MANAGER_ROLE_ORDER = 1;
    private static final int DEFAULT_MEMBER_ROLE_ORDER = 2;

    private final GroupRepository groupRepository;

    public Group create(Long memberId, Long boardId, GroupCreateRequest request) {
        Group group = Group.create(
                boardId,
                request.name(),
                request.description(),
                request.coverImageUrl(),
                request.category(),
                request.activityType(),
                request.region(),
                request.visibility(),
                memberId,
                request.maxMembers(),
                request.interestIds()
        );
        Group savedGroup = groupRepository.save(group);
        initializeDefaultRoles(savedGroup, memberId);
        return savedGroup;
    }

    public void update(Group group, GroupUpdateRequest request) {
        group.update(
                request.name(),
                request.description(),
                request.coverImageUrl(),
                request.category(),
                request.activityType(),
                request.region(),
                request.visibility(),
                request.maxMembers(),
                request.interestIds()
        );
    }

    public void updateMember(Group group, Long memberId, GroupMemberUpdateRequest request) {
        group.updateMember(memberId, request.role(), request.customTitleId());
    }

    public void leave(Group group, Long memberId) {
        group.leave(memberId);
    }

    public void removeMember(Group group, Long memberId) {
        group.removeMember(memberId);
    }

    public void transferOwner(Group group, GroupOwnerTransferRequest request) {
        group.transferOwner(request.newOwnerMemberId());
    }

    public GroupRole createRole(Group group, GroupRoleCreateRequest request) {
        return group.addRole(request.title(), request.displayOrderOrDefault());
    }

    public GroupRole updateRole(Group group, Long roleId, GroupRoleUpdateRequest request) {
        group.updateRole(roleId, request.title(), request.displayOrderOrDefault());
        return group.getRoles().stream()
                .filter(role -> role.getId().equals(roleId))
                .findFirst()
                .orElseThrow();
    }

    public void deleteRole(Group group, Long roleId) {
        group.deleteRole(roleId);
    }

    public void close(Group group) {
        group.close();
    }

    private void initializeDefaultRoles(Group group, Long ownerId) {
        GroupRole managerRole = group.addRole(DEFAULT_MANAGER_ROLE_TITLE, DEFAULT_MANAGER_ROLE_ORDER);
        group.addRole(DEFAULT_MEMBER_ROLE_TITLE, DEFAULT_MEMBER_ROLE_ORDER);
        groupRepository.flush();
        group.assignCustomTitle(ownerId, managerRole.getId());
    }
}
