package cluverse.post.repository.dto;

import java.time.LocalDateTime;

public record LatestPostCacheEntry(
        Long postId,
        LocalDateTime createdAt
) {
}
