package cluverse.group.service.response;

import cluverse.group.domain.Group;
import cluverse.group.domain.GroupActivityType;
import cluverse.group.domain.GroupCategory;
import cluverse.group.domain.GroupStatus;
import cluverse.group.domain.GroupVisibility;

import java.util.List;

public record GroupSummaryResponse(
        Long groupId,
        String name,
        String description,
        String coverImageUrl,
        GroupCategory category,
        GroupActivityType activityType,
        String region,
        GroupVisibility visibility,
        GroupStatus status,
        Integer maxMembers,
        int memberCount,
        boolean recruiting,
        long openRecruitmentCount,
        List<GroupInterestResponse> interests
) {
    public GroupSummaryResponse {
        interests = interests == null ? List.of() : List.copyOf(interests);
    }

    public static GroupSummaryResponse of(Group group,
                                          boolean recruiting,
                                          long openRecruitmentCount,
                                          List<GroupInterestResponse> interests) {
        return new GroupSummaryResponse(
                group.getId(),
                group.getName(),
                group.getDescription(),
                group.getCoverImageUrl(),
                group.getCategory(),
                group.getActivityType(),
                group.getRegion(),
                group.getVisibility(),
                group.getStatus(),
                group.getMaxMembers(),
                group.getMemberCount(),
                recruiting,
                openRecruitmentCount,
                interests
        );
    }
}
