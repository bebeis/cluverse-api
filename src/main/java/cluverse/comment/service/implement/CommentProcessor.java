package cluverse.comment.service.implement;

import cluverse.comment.domain.Comment;
import cluverse.comment.exception.CommentExceptionMessage;
import cluverse.comment.service.request.CommentCreateRequest;
import cluverse.common.exception.ForbiddenException;
import cluverse.member.service.implement.MemberReader;
import cluverse.meta.service.implement.PostMetaWriter;
import cluverse.post.service.implement.PostAccessReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class CommentProcessor {

    private final CommentReader commentReader;
    private final CommentWriter commentWriter;
    private final MemberReader memberReader;
    private final PostAccessReader postAccessReader;
    private final PostMetaWriter postMetaWriter;
    private final PostCommentActivityWriter postCommentActivityWriter;

    public Long createComment(Long memberId, Long postId, CommentCreateRequest request, String clientIp) {
        postAccessReader.validateWritablePost(memberId, postId);
        Comment parentComment = resolveParentComment(postId, request.parentCommentId());
        Comment comment = commentWriter.create(memberId, postId, parentComment, request, clientIp);
        postMetaWriter.increaseCommentCount(postId);
        postCommentActivityWriter.reflectCreated(comment);

        if (parentComment != null) {
            commentWriter.increaseReplyCount(parentComment.getId());
        }

        return comment.getId();
    }

    public Long deleteComment(Long memberId, Long commentId) {
        Comment comment = commentReader.readForUpdateOrThrow(commentId);
        validateDeletePermission(memberId, comment);
        if (comment.isActive()) {
            delete(comment);
            postCommentActivityWriter.reflectDeleted(comment.getPostId(), comment.getId());
        }
        return comment.getPostId();
    }

    private Comment resolveParentComment(Long postId, Long parentCommentId) {
        if (parentCommentId == null) {
            return null;
        }

        Comment parentComment = commentReader.readForUpdateOrThrow(parentCommentId);
        commentReader.validateBelongsToPost(parentComment, postId);
        commentReader.validateReplyWritable(parentComment);
        return parentComment;
    }

    private void validateDeletePermission(Long memberId, Comment comment) {
        if (comment.isAuthor(memberId) || memberReader.isAdmin(memberId)) {
            return;
        }
        throw new ForbiddenException(CommentExceptionMessage.COMMENT_ACCESS_DENIED.getMessage());
    }

    private void delete(Comment comment) {
        if (commentReader.hasChildren(comment)) {
            commentWriter.delete(comment);
            return;
        }
        deletePhysically(comment);
    }

    private void deletePhysically(Comment comment) {
        Long parentId = comment.getParentId();

        commentWriter.remove(comment);
        postMetaWriter.decreaseCommentCount(comment.getPostId());
        if (parentId != null) {
            commentWriter.decreaseReplyCount(parentId);
            deleteParentIfRemovable(parentId);
        }
    }

    private void deleteParentIfRemovable(Long parentId) {
        commentReader.readForUpdate(parentId)
                .filter(Comment::isDeleted)
                .filter(parent -> !commentReader.hasChildren(parent))
                .ifPresent(this::deletePhysically);
    }
}
