package cluverse.feed.service.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record HomeFeedSearchRequest(
        HomeFeedFilter filter,

        @Size(max = 200, message = "cursor는 200자 이하여야 합니다.")
        String cursor,

        @Min(value = 1, message = "limit은 1 이상이어야 합니다.")
        @Max(value = 50, message = "limit은 50 이하여야 합니다.")
        Integer limit
) {
    private static final int DEFAULT_LIMIT = 20;
    private static final HomeFeedFilter DEFAULT_FILTER = HomeFeedFilter.ALL;

    public HomeFeedFilter filterOrDefault() {
        return filter == null ? DEFAULT_FILTER : filter;
    }

    public int limitOrDefault() {
        return limit == null ? DEFAULT_LIMIT : limit;
    }
}
