package cluverse.reaction.service.response;

public record PostBookmarkResponse(
        Long postId,
        boolean bookmarked
) {

    public static PostBookmarkResponse bookmark(Long postId) {
        return new PostBookmarkResponse(postId, true);
    }

    public static PostBookmarkResponse remove(Long postId) {
        return new PostBookmarkResponse(postId, false);
    }
}
