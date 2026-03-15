package cluverse.comment.service.response;

import cluverse.comment.domain.CommentStatus;

public record CommentDeleteResponse(
        Long postId,
        Long commentId,
        CommentStatus status
) {
    public static CommentDeleteResponse delete(Long postId, Long commentId, CommentStatus status) {
        return new CommentDeleteResponse(postId, commentId, status);
    }
}
