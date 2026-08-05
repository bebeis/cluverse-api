package cluverse.popularity.service.implement;

import java.time.LocalDateTime;

public record PopularPostView(
        Long postId,
        Long boardId,
        String title,
        long score,
        long likeCount,
        long commentCount,
        LocalDateTime promotedAt,
        LocalDateTime finalizedAt
) {
}
