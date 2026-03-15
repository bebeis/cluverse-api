package cluverse.comment.service.response;

public record CommentReactionTargetResponse(
        Long postId,
        Long commentId
) {
}
