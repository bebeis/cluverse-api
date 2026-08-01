package cluverse.popularity.repository.dto;

import java.time.LocalDateTime;

public record PopularPostSummary(
        Long postId,
        Long boardId,
        String title,
        long score,
        long likeCount,
        long commentCount,
        long viewCount,
        LocalDateTime promotedAt,
        LocalDateTime finalizedAt
) {
}
