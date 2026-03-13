package cluverse.post.service.request;

import cluverse.post.domain.PostCategory;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record PostSearchRequest(
        // 없으면 전체 게시판에서 검색, 있으면 해당 게시판에서 검색
        Long boardId,

        // 없으면 전체 카테고리에서 검색, 있으면 해당 카테고리에서 검색
        PostCategory category,

        @Size(max = 50, message = "태그는 50자 이하여야 합니다.")
        String tag,

        PostSortType sort,

        @Min(value = 1, message = "페이지는 1 이상이어야 합니다.")
        Integer page,

        @Min(value = 1, message = "조회 건수는 1 이상이어야 합니다.")
        @Max(value = 100, message = "조회 건수는 100 이하여야 합니다.")
        Integer pageSize
) {
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final PostSortType DEFAULT_SORT = PostSortType.LATEST;

    public int pageOrDefault() {
        return page == null ? DEFAULT_PAGE : page;
    }

    public int sizeOrDefault() {
        return pageSize == null ? DEFAULT_SIZE : pageSize;
    }

    public PostSortType sortOrDefault() {
        return sort == null ? DEFAULT_SORT : sort;
    }
}
