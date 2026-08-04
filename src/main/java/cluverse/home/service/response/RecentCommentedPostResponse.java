package cluverse.home.service.response;

import cluverse.home.service.implement.RecentCommentedPostView;

import java.time.LocalDateTime;

public record RecentCommentedPostResponse(
        Long postId,
        String title,
        LocalDateTime lastCommentedAt
) {
    public static RecentCommentedPostResponse from(RecentCommentedPostView post) {
        return new RecentCommentedPostResponse(post.postId(), post.title(), post.lastCommentedAt());
    }
}
