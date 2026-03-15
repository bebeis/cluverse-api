package cluverse.reaction.service.response;

public record PostLikeResponse(
        Long postId,
        boolean liked
) {

    public static PostLikeResponse like(Long postId) {
        return new PostLikeResponse(postId, true);
    }

    public static PostLikeResponse unlike(Long postId) {
        return new PostLikeResponse(postId, false);
    }
}
