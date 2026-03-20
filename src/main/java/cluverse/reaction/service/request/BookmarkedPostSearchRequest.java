package cluverse.reaction.service.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record BookmarkedPostSearchRequest(
        BookmarkedPostSortType sort,

        @Min(value = 1, message = "page는 1 이상이어야 합니다.")
        @Max(value = 500, message = "page는 500 이하여야 합니다.")
        Integer page,

        @Min(value = 1, message = "size는 1 이상이어야 합니다.")
        @Max(value = 50, message = "size는 50 이하여야 합니다.")
        Integer size
) {
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final BookmarkedPostSortType DEFAULT_SORT = BookmarkedPostSortType.BOOKMARKED_AT;

    public BookmarkedPostSortType sortOrDefault() {
        return sort == null ? DEFAULT_SORT : sort;
    }

    public int pageOrDefault() {
        return page == null ? DEFAULT_PAGE : page;
    }

    public int sizeOrDefault() {
        return size == null ? DEFAULT_SIZE : size;
    }
}
