package cluverse.group.service.response;

import cluverse.group.domain.GroupRole;

public record GroupRoleResponse(
        Long groupRoleId,
        String title,
        int displayOrder
) {
    public static GroupRoleResponse from(GroupRole role) {
        return new GroupRoleResponse(role.getId(), role.getTitle(), role.getDisplayOrder());
    }
}
