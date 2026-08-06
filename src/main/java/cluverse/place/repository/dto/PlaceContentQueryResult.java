package cluverse.place.repository.dto;

import java.time.LocalDateTime;

public record PlaceContentQueryResult(
        String contentType,
        Long contentId,
        Long postId,
        String title,
        String content,
        Long authorId,
        String authorNickname,
        boolean isLocalStudent,
        LocalDateTime createdAt
) {
}
