package cluverse.popularity.service.response;

import java.time.LocalDateTime;

public record PopularPostResponse(
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
    public static PopularPostResponse of(
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
        return new PopularPostResponse(
                postId,
                boardId,
                title,
                score,
                likeCount,
                commentCount,
                viewCount,
                promotedAt,
                finalizedAt
        );
    }
}
