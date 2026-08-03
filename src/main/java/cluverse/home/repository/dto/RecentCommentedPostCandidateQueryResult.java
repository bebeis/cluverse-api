package cluverse.home.repository.dto;

import java.time.LocalDateTime;

public record RecentCommentedPostCandidateQueryResult(
        Long postId,
        LocalDateTime lastCommentedAt
) {
}
