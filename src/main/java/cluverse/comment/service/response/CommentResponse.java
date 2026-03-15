package cluverse.comment.service.response;

import cluverse.comment.domain.CommentStatus;
import cluverse.comment.repository.dto.CommentQueryDto;

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
    private static final String BLOCKED_CONTENT = "차단한 사용자의 댓글입니다.";
    private static final String DELETED_CONTENT = "삭제된 댓글입니다.";
    private static final String BLINDED_CONTENT = "블라인드 처리된 댓글입니다.";

    public static CommentResponse from(CommentQueryDto comment, Long viewerId) {
        boolean isMine = viewerId != null && viewerId.equals(comment.authorMemberId());

        return new CommentResponse(
                comment.commentId(),
                comment.postId(),
                comment.parentCommentId(),
                comment.depth(),
                resolveContent(comment),
                comment.status(),
                comment.isAnonymous(),
                isMine,
                comment.likedByMe(),
                comment.blockedAuthor(),
                comment.likeCount(),
                comment.replyCount(),
                resolveAuthor(comment, isMine),
                comment.createdAt(),
                comment.updatedAt()
        );
    }

    private static String resolveContent(CommentQueryDto comment) {
        if (comment.blockedAuthor()) {
            return BLOCKED_CONTENT;
        }
        return switch (comment.status()) {
            case DELETED -> DELETED_CONTENT;
            case BLINDED -> BLINDED_CONTENT;
            case ACTIVE -> comment.content();
        };
    }

    private static CommentAuthorResponse resolveAuthor(CommentQueryDto comment, boolean isMine) {
        if (comment.blockedAuthor()) {
            return CommentAuthorResponse.blocked();
        }
        return CommentAuthorResponse.visibleOf(
                comment.isAnonymous(),
                isMine,
                comment.authorMemberId(),
                comment.authorNickname(),
                comment.authorProfileImageUrl()
        );
    }
}
