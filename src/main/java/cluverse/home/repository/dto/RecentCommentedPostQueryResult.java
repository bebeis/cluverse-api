package cluverse.home.repository.dto;

import java.time.LocalDateTime;

public record RecentCommentedPostQueryResult(
        Long postId,
        String title,
        LocalDateTime lastCommentedAt
) {
}
