package cluverse.comment.service.implement;

import cluverse.comment.domain.Comment;
import cluverse.comment.domain.CommentPageCursor;
import cluverse.comment.exception.CommentExceptionMessage;
import cluverse.comment.repository.CommentQueryRepository;
import cluverse.comment.repository.CommentRepository;
import cluverse.comment.repository.dto.CommentPageQueryResult;
import cluverse.comment.repository.dto.CommentQueryDto;
import cluverse.comment.service.request.CommentPageRequest;
import cluverse.comment.service.response.CommentLastRepliedPost;
import cluverse.comment.service.response.CommentReactionTargetResponse;
import cluverse.common.exception.BadRequestException;
import cluverse.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentReader {

    private final CommentRepository commentRepository;
    private final CommentQueryRepository commentQueryRepository;

    public Comment readOrThrow(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException(CommentExceptionMessage.COMMENT_NOT_FOUND.getMessage()));
    }

    public Comment readActiveOrThrow(Long commentId) {
        Comment comment = readOrThrow(commentId);
        validateActive(comment);
        return comment;
    }

    public Optional<Comment> read(Long commentId) {
        return commentRepository.findById(commentId);
    }

    public Optional<Long> findIdByRequestId(Long memberId, String requestId) {
        return commentRepository.findByMemberIdAndClientRequestId(memberId, requestId).map(Comment::getId);
    }

    public CommentReactionTargetResponse readReactionTarget(Long commentId) {
        Comment comment = readActiveOrThrow(commentId);
        return new CommentReactionTargetResponse(comment.getPostId(), comment.getId());
    }

    public List<CommentLastRepliedPost> readRecentCommentRepliedPosts(Long size) {
        return commentQueryRepository.findRecentCommentRepliedPosts(size);
    }

    public long readMaxCommentId() {
        return commentQueryRepository.findMaxCommentId();
    }

    public CommentPageQueryResult readCommentPageV1(Long viewerId, CommentPageRequest request,
                                                    CommentPageCursor cursor) {
        return commentQueryRepository.findCommentPageV1(viewerId, request, cursor);
    }

    public CommentPageQueryResult readCommentPageV2(Long viewerId, CommentPageRequest request,
                                                    CommentPageCursor cursor) {
        return commentQueryRepository.findCommentPageV2(viewerId, request, cursor);
    }

    public CommentQueryDto readComment(Long viewerId, Long commentId) {
        return commentQueryRepository.findComment(viewerId, commentId);
    }

    public boolean hasChildren(Comment comment) {
        return commentRepository.existsByParentId(comment.getId());
    }

    public void validateBelongsToPost(Comment comment, Long postId) {
        if (!comment.getPostId().equals(postId)) {
            throw new BadRequestException(CommentExceptionMessage.COMMENT_PARENT_POST_MISMATCH.getMessage());
        }
    }

    public void validateReplyWritable(Comment comment) {
        if (!comment.isActive()) {
            throw new BadRequestException(CommentExceptionMessage.COMMENT_REPLY_NOT_ALLOWED.getMessage());
        }
    }

    private void validateActive(Comment comment) {
        if (!comment.isActive()) {
            throw new NotFoundException(CommentExceptionMessage.COMMENT_NOT_FOUND.getMessage());
        }
    }
}
