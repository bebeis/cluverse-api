package cluverse.board.service.request;

import cluverse.board.domain.BoardType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record BoardSearchRequest(
        BoardType type,

        @Size(max = 100, message = "검색어는 100자 이하여야 합니다.")
        String keyword,

        Long parentBoardId,

        @Min(value = 0, message = "탐색 깊이는 0 이상이어야 합니다.")
        @Max(value = 2, message = "탐색 깊이는 2 이하여야 합니다.")
        Integer depth,

        Boolean activeOnly
) {
    private static final int DEFAULT_DEPTH = 2;

    public int depthOrDefault() {
        return depth == null ? DEFAULT_DEPTH : depth;
    }

    public boolean activeOnlyOrDefault() {
        return activeOnly == null || activeOnly;
    }
}
