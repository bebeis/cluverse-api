package cluverse.comment.repository.dto;

import cluverse.comment.domain.CommentStatus;

import java.time.LocalDateTime;

public record CommentQueryDto(
        Long commentId,
        Long postId,
        Long parentCommentId,
        int depth,
        String content,
        CommentStatus status,
        boolean isAnonymous,
        long likeCount,
        long replyCount,
        Long authorMemberId,
        String authorNickname,
        String authorProfileImageUrl,
        boolean likedByMe,
        boolean blockedAuthor,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
