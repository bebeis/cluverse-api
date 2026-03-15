package cluverse.reaction.service.response;

public record CommentLikeResponse(
        Long postId,
        Long commentId,
        boolean liked
) {
    public static CommentLikeResponse like(Long postId, Long commentId) {
        return new CommentLikeResponse(postId, commentId, true);
    }

    public static CommentLikeResponse unlike(Long postId, Long commentId) {
        return new CommentLikeResponse(postId, commentId, false);
    }
}
