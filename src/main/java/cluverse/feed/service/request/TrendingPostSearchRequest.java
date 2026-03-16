package cluverse.feed.service.request;

import cluverse.post.domain.PostCategory;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record TrendingPostSearchRequest(
        TrendingRangeType range,
        PostCategory category,

        @Size(max = 200, message = "cursor는 200자 이하여야 합니다.")
        String cursor,

        @Min(value = 1, message = "limit은 1 이상이어야 합니다.")
        @Max(value = 50, message = "limit은 50 이하여야 합니다.")
        Integer limit
) {
    private static final int DEFAULT_LIMIT = 20;
    private static final TrendingRangeType DEFAULT_RANGE = TrendingRangeType.DAY_7;

    public TrendingRangeType rangeOrDefault() {
        return range == null ? DEFAULT_RANGE : range;
    }

    public int limitOrDefault() {
        return limit == null ? DEFAULT_LIMIT : limit;
    }
}
