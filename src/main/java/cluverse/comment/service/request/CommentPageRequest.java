package cluverse.comment.service.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CommentPageRequest(
        @NotNull(message = "postId를 입력해주세요.")
        Long postId,

        Long parentCommentId,

        @Min(value = 0, message = "offset은 0 이상이어야 합니다.")
        Integer offset,

        @Min(value = 1, message = "limit은 1 이상이어야 합니다.")
        @Max(value = 100, message = "limit은 100 이하여야 합니다.")
        Integer limit
) {
    private static final int DEFAULT_OFFSET = 0;
    private static final int DEFAULT_LIMIT = 20;

    public CommentPageRequest {
        offset = offset == null ? DEFAULT_OFFSET : offset;
        limit = limit == null ? DEFAULT_LIMIT : limit;
    }
}
