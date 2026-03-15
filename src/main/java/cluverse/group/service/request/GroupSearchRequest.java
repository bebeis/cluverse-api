package cluverse.group.service.request;

import cluverse.group.domain.GroupActivityType;
import cluverse.group.domain.GroupCategory;
import cluverse.group.domain.GroupVisibility;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record GroupSearchRequest(
        @Size(max = 100, message = "검색어는 100자 이하여야 합니다.")
        String keyword,

        GroupCategory category,

        GroupActivityType activityType,

        @Size(max = 50, message = "지역은 50자 이하여야 합니다.")
        String region,

        GroupVisibility visibility,

        Boolean recruitableOnly,

        @Min(value = 1, message = "페이지는 1 이상이어야 합니다.")
        @Max(value = 500, message = "페이지는 500 이하여야 합니다.")
        Integer page,

        @Min(value = 1, message = "조회 건수는 1 이상이어야 합니다.")
        @Max(value = 100, message = "조회 건수는 100 이하여야 합니다.")
        Integer size
) {
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;

    public int pageOrDefault() {
        return page == null ? DEFAULT_PAGE : page;
    }

    public int sizeOrDefault() {
        return size == null ? DEFAULT_SIZE : size;
    }
}
