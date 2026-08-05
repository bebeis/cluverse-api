package cluverse.post.service.response;

import cluverse.meta.service.implement.ViewCountResult;
import cluverse.meta.service.implement.ViewCountSource;

public record PostViewCountResponse(
        Long postId,
        long viewCount,
        boolean counted,
        ViewCountSource source
) {
    public static PostViewCountResponse of(Long postId, ViewCountResult result) {
        return new PostViewCountResponse(postId, result.viewCount(), result.counted(), result.source());
    }
}
