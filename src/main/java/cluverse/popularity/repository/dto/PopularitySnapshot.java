package cluverse.popularity.repository.dto;

import java.time.LocalDateTime;

public record PopularitySnapshot(
        Long postId,
        Long boardId,
        LocalDateTime createdAt,
        long likeCount,
        long commentCount
) {
}
