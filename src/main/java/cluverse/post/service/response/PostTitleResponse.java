package cluverse.post.service.response;

import java.time.LocalDateTime;

public record PostTitleResponse(
        Long postId,
        String title,
        LocalDateTime lastCommentRepliedAt
) {
}
