package cluverse.comment.service.implement;

import cluverse.comment.domain.Comment;
import cluverse.comment.exception.CommentExceptionMessage;
import cluverse.comment.repository.CommentRepository;
import cluverse.comment.service.request.CommentCreateRequest;
import cluverse.common.exception.BadRequestException;
import cluverse.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class CommentWriter {

    private final CommentRepository commentRepository;

    public Comment create(Long memberId, Long postId, Comment parentComment, CommentCreateRequest request, String clientIp) {
        int depth = parentComment == null ? 0 : parentComment.getDepth() + 1;
        validateDepth(depth);

        Comment comment = Comment.createByMember(
                postId,
                memberId,
                parentComment == null ? null : parentComment.getId(),
                depth,
                request.content(),
                request.isAnonymous(),
                clientIp
        );
        return commentRepository.save(comment);
    }

    public void delete(Comment comment) {
        comment.delete();
    }

    public void remove(Comment comment) {
        commentRepository.delete(comment);
    }

    public void increaseLikeCount(Long commentId) {
        validateUpdated(commentRepository.increaseLikeCount(commentId), CommentExceptionMessage.COMMENT_NOT_FOUND);
    }

    public void decreaseLikeCount(Long commentId) {
        validateUpdated(commentRepository.decreaseLikeCount(commentId), CommentExceptionMessage.COMMENT_LIKE_COUNT_ALREADY_ZERO);
    }

    public void increaseReplyCount(Long commentId) {
        validateUpdated(commentRepository.increaseReplyCount(commentId), CommentExceptionMessage.COMMENT_NOT_FOUND);
    }

    public void decreaseReplyCount(Long commentId) {
        validateUpdated(commentRepository.decreaseReplyCount(commentId), CommentExceptionMessage.COMMENT_REPLY_COUNT_ALREADY_ZERO);
    }

    private void validateDepth(int depth) {
        if (depth > 5) {
            throw new BadRequestException(CommentExceptionMessage.COMMENT_MAX_DEPTH_EXCEEDED.getMessage());
        }
    }

    private void validateUpdated(int updatedRowCount, CommentExceptionMessage message) {
        if (updatedRowCount == 0) {
            if (message == CommentExceptionMessage.COMMENT_NOT_FOUND) {
                throw new NotFoundException(message.getMessage());
            }
            throw new BadRequestException(message.getMessage());
        }
    }
}
