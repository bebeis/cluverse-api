package cluverse.board.service.response;

import cluverse.group.domain.GroupMemberRole;
import cluverse.group.domain.GroupVisibility;

public record GroupBoardSummaryResponse(
        Long groupId,
        String groupName,
        GroupVisibility visibility,
        boolean isMember,
        GroupMemberRole myRole
) {
}
