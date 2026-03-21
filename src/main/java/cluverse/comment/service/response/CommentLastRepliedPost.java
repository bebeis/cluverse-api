package cluverse.comment.service.response;

import java.time.LocalDateTime;

public record CommentLastRepliedPost(
        Long postId,
        LocalDateTime lastCommentRepliedAt
) {
}
