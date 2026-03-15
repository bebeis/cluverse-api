package cluverse.board.repository.dto;

import cluverse.group.domain.GroupMemberRole;
import cluverse.group.domain.GroupStatus;
import cluverse.group.domain.GroupVisibility;

public record BoardGroupQueryDto(
        Long boardId,
        Long groupId,
        String groupName,
        GroupVisibility visibility,
        GroupStatus status,
        GroupMemberRole myRole
) {
    public boolean isMember() {
        return myRole != null;
    }
}
