package cluverse.comment.service.request;

import jakarta.validation.constraints.NotBlank;

public record CommentCreateRequest(
        Long parentCommentId,

        @NotBlank(message = "댓글 내용을 입력해주세요.")
        String content,

        boolean isAnonymous
) {
}
