package cluverse.place.service.response;

import cluverse.place.repository.dto.PlaceContentQueryResult;

import java.time.LocalDateTime;

public record PlaceContentResponse(
        PlaceContentType contentType,
        Long contentId,
        Long postId,
        String title,
        String content,
        Long authorId,
        String authorNickname,
        boolean isLocalStudent,
        LocalDateTime createdAt
) {
    public static PlaceContentResponse from(PlaceContentQueryResult result) {
        return new PlaceContentResponse(
                PlaceContentType.valueOf(result.contentType()), result.contentId(), result.postId(), result.title(),
                result.content(), result.authorId(), result.authorNickname(), result.isLocalStudent(),
                result.createdAt()
        );
    }
}
