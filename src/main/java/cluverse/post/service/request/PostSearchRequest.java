package cluverse.post.service.request;

import cluverse.post.domain.PostCategory;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record PostSearchRequest(
        @NotNull(message = "게시판 ID를 입력해주세요.")
        Long boardId,

        PostCategory category,

        PostSortType sort,

        @Min(value = 1, message = "페이지는 1 이상이어야 합니다.")
        @Max(value = 500, message = "페이지는 500 이하여야 합니다. 더 오래된 글은 날짜 기반 조회를 이용해주세요.")
        Integer page,

        @Min(value = 1, message = "조회 건수는 1 이상이어야 합니다.")
        @Max(value = 100, message = "조회 건수는 100 이하여야 합니다.")
        Integer size,

        LocalDate date
) {
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final PostSortType DEFAULT_SORT = PostSortType.LATEST;

    public int pageOrDefault() {
        return page == null ? DEFAULT_PAGE : page;
    }

    public int sizeOrDefault() {
        return size == null ? DEFAULT_SIZE : size;
    }

    public PostSortType sortOrDefault() {
        return sort == null ? DEFAULT_SORT : sort;
    }

    public boolean isDateBased() {
        return date != null;
    }

    @AssertTrue(message = "카테고리 필터와 정렬 조건은 함께 사용할 수 없습니다.")
    public boolean isCategorySortable() {
        return category == null || sort == null;
    }

    @AssertTrue(message = "날짜 기반 조회는 최신순(LATEST) 정렬에서만 사용할 수 있습니다.")
    public boolean isDateSortable() {
        return date == null || sort == null || sort == PostSortType.LATEST;
    }

    @AssertTrue(message = "날짜 기반 조회와 페이지 기반 조회는 함께 사용할 수 없습니다.")
    public boolean isDatePageExclusive() {
        return date == null || page == null;
    }
}
