package cluverse.group.service.request;

import cluverse.group.domain.GroupActivityType;
import cluverse.group.domain.GroupCategory;
import cluverse.group.domain.GroupVisibility;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record GroupCreateRequest(
        @NotBlank(message = "그룹명을 입력해주세요.")
        @Size(max = 100, message = "그룹명은 100자 이하여야 합니다.")
        String name,

        @Size(max = 3000, message = "그룹 소개는 3000자 이하여야 합니다.")
        String description,

        @Size(max = 500, message = "대표 이미지 URL은 500자 이하여야 합니다.")
        String coverImageUrl,

        @NotNull(message = "그룹 카테고리를 선택해주세요.")
        GroupCategory category,

        @NotNull(message = "활동 방식을 선택해주세요.")
        GroupActivityType activityType,

        @Size(max = 50, message = "활동 지역은 50자 이하여야 합니다.")
        String region,

        @NotNull(message = "공개 범위를 선택해주세요.")
        GroupVisibility visibility,

        @Min(value = 1, message = "최대 인원은 1명 이상이어야 합니다.")
        Integer maxMembers,

        @Size(max = 10, message = "관심 태그는 최대 10개까지 선택할 수 있습니다.")
        List<@NotNull(message = "관심 태그 ID는 비어 있을 수 없습니다.") Long> interestIds
) {
    public GroupCreateRequest {
        interestIds = interestIds == null ? List.of() : List.copyOf(interestIds);
    }
}
