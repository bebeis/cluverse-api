package cluverse.member.service.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record MemberPostPageRequest(
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
