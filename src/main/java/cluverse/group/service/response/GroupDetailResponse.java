package cluverse.group.service.response;

import cluverse.group.domain.GroupActivityType;
import cluverse.group.domain.GroupCategory;
import cluverse.group.domain.GroupMemberRole;
import cluverse.group.domain.GroupStatus;
import cluverse.group.domain.GroupVisibility;

import java.time.LocalDateTime;
import java.util.List;

public record GroupDetailResponse(
        Long groupId,
        Long boardId,
        String name,
        String description,
        String coverImageUrl,
        GroupCategory category,
        GroupActivityType activityType,
        String region,
        GroupVisibility visibility,
        GroupStatus status,
        Long ownerId,
        String ownerNickname,
        Integer maxMembers,
        int memberCount,
        boolean member,
        GroupMemberRole myRole,
        long openRecruitmentCount,
        List<GroupInterestResponse> interests,
        List<GroupRoleResponse> roles,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public GroupDetailResponse {
        interests = interests == null ? List.of() : List.copyOf(interests);
        roles = roles == null ? List.of() : List.copyOf(roles);
    }
}
