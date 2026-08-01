package cluverse.comment.service.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CommentPageRequest(
        @NotNull(message = "postId를 입력해주세요.")
        Long postId,

        Long parentCommentId,

        @Size(max = 1024, message = "cursor는 1024자 이하여야 합니다.")
        String cursor,

        @Min(value = 1, message = "limit은 1 이상이어야 합니다.")
        @Max(value = 100, message = "limit은 100 이하여야 합니다.")
        Integer limit
) {
    private static final int DEFAULT_LIMIT = 20;

    public CommentPageRequest {
        cursor = cursor == null || cursor.isBlank() ? null : cursor;
        limit = limit == null ? DEFAULT_LIMIT : limit;
    }
}
