package cluverse.home.service.implement;

import cluverse.home.repository.dto.RecentCommentedPostQueryResult;

import java.time.LocalDateTime;

public record RecentCommentedPostView(
        Long postId,
        String title,
        LocalDateTime lastCommentedAt
) {
    public static RecentCommentedPostView from(RecentCommentedPostQueryResult result) {
        return new RecentCommentedPostView(result.postId(), result.title(), result.lastCommentedAt());
    }
}
