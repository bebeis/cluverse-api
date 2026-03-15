package cluverse.group.service.response;

import cluverse.group.domain.GroupCategory;
import cluverse.group.domain.GroupMemberRole;
import cluverse.group.domain.GroupVisibility;

public record MyGroupSummaryResponse(
        Long groupId,
        String name,
        GroupCategory category,
        GroupVisibility visibility,
        GroupMemberRole myRole,
        int memberCount,
        long openRecruitmentCount
) {
}
