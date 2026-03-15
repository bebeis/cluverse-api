package cluverse.group.service.response;

public record GroupRoleResponse(
        Long groupRoleId,
        String title,
        int displayOrder
) {
}
