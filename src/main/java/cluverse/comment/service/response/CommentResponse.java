package cluverse.comment.service.response;

import cluverse.comment.domain.CommentStatus;

import java.time.LocalDateTime;

public record CommentResponse(
        Long commentId,
        Long postId,
        Long parentCommentId,
        int depth,
        String content,
        CommentStatus status,
        boolean isAnonymous,
        boolean isMine,
        boolean likedByMe,
        boolean blockedAuthor,
        long likeCount,
        long replyCount,
        CommentAuthorResponse author,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
