package cluverse.group.service.response;

import cluverse.group.domain.Group;
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
    public static MyGroupSummaryResponse of(Group group, GroupMemberRole myRole, long openRecruitmentCount) {
        return new MyGroupSummaryResponse(
                group.getId(),
                group.getName(),
                group.getCategory(),
                group.getVisibility(),
                myRole,
                group.getMemberCount(),
                openRecruitmentCount
        );
    }
}
