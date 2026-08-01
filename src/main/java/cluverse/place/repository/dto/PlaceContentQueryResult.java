package cluverse.place.repository.dto;

import java.time.LocalDateTime;

public record PlaceContentQueryResult(
        String contentType,
        Long contentId,
        String title,
        String content,
        Long authorId,
        String authorNickname,
        LocalDateTime createdAt
) {
}
