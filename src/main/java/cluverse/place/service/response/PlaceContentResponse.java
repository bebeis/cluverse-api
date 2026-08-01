package cluverse.place.service.response;

import cluverse.place.repository.dto.PlaceContentQueryResult;

import java.time.LocalDateTime;

public record PlaceContentResponse(
        PlaceContentType contentType,
        Long contentId,
        String title,
        String content,
        Long authorId,
        String authorNickname,
        LocalDateTime createdAt
) {
    public static PlaceContentResponse from(PlaceContentQueryResult result) {
        return new PlaceContentResponse(
                PlaceContentType.valueOf(result.contentType()), result.contentId(), result.title(), result.content(),
                result.authorId(), result.authorNickname(), result.createdAt()
        );
    }
}
