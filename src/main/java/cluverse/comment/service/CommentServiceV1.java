package cluverse.comment.service;

import cluverse.comment.domain.Comment;
import cluverse.comment.exception.CommentExceptionMessage;
import cluverse.comment.repository.CommentQueryRepository;
import cluverse.comment.repository.dto.CommentPageQueryResult;
import cluverse.comment.repository.dto.CommentQueryDto;
import cluverse.comment.service.implement.CommentReader;
import cluverse.comment.service.implement.CommentWriter;
import cluverse.comment.service.request.CommentCreateRequest;
import cluverse.comment.service.request.CommentPageRequest;
import cluverse.comment.service.response.CommentDeleteResponse;
import cluverse.comment.service.response.CommentPageResponse;
import cluverse.comment.service.response.CommentReactionTargetResponse;
import cluverse.comment.service.response.CommentResponse;
import cluverse.common.exception.ForbiddenException;
import cluverse.member.service.MemberService;
import cluverse.meta.service.PostMetaService;
import cluverse.post.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CommentServiceV1 implements CommentService {

    private final CommentReader commentReader;
    private final CommentWriter commentWriter;
    private final CommentQueryRepository commentQueryRepository;
    private final PostService postService;
    private final PostMetaService postMetaService;
    private final MemberService memberService;

    @Override
    @Transactional(readOnly = true)
    public CommentPageResponse getComments(Long memberId, CommentPageRequest request) {
        postService.validateReadablePost(memberId, request.postId());
        validateParentComment(request.postId(), request.parentCommentId());

        CommentPageQueryResult queryResult = commentQueryRepository.findCommentPage(memberId, request);
        List<CommentResponse> comments = queryResult.comments().stream()
                .map(comment -> CommentResponse.from(comment, memberId))
                .toList();

        return new CommentPageResponse(comments, request.offset(), request.limit(), queryResult.hasNext());
    }

    @Override
    public CommentResponse createComment(Long memberId, Long postId, CommentCreateRequest request, String clientIp) {
        postService.validateWritablePost(memberId, postId);
        Comment parentComment = resolveParentComment(postId, request.parentCommentId());
        Comment comment = commentWriter.create(memberId, postId, parentComment, request, clientIp);

        postMetaService.increaseCommentCount(postId);
        if (parentComment != null) {
            commentWriter.increaseReplyCount(parentComment.getId());
        }

        CommentQueryDto commentQueryDto = commentQueryRepository.findComment(memberId, comment.getId());
        return CommentResponse.from(commentQueryDto, memberId);
    }

    @Override
    public CommentDeleteResponse deleteComment(Long memberId, Long commentId) {
        Comment comment = commentReader.readOrThrow(commentId);
        validateDeletePermission(memberId, comment);
        if (comment.isActive()) {
            delete(comment);
        }
        return CommentDeleteResponse.delete(comment.getPostId(), comment.getId(), cluverse.comment.domain.CommentStatus.DELETED);
    }

    @Override
    @Transactional(readOnly = true)
    public CommentReactionTargetResponse getReactionTarget(Long commentId) {
        Comment comment = commentReader.readActiveOrThrow(commentId);
        return new CommentReactionTargetResponse(comment.getPostId(), comment.getId());
    }

    @Override
    public void increaseLikeCount(Long commentId) {
        commentWriter.increaseLikeCount(commentId);
    }

    @Override
    public void decreaseLikeCount(Long commentId) {
        commentWriter.decreaseLikeCount(commentId);
    }

    private void validateParentComment(Long postId, Long parentCommentId) {
        if (parentCommentId == null) {
            return;
        }
        Comment parentComment = commentReader.readOrThrow(parentCommentId);
        commentReader.validateBelongsToPost(parentComment, postId);
    }

    private Comment resolveParentComment(Long postId, Long parentCommentId) {
        if (parentCommentId == null) {
            return null;
        }

        Comment parentComment = commentReader.readOrThrow(parentCommentId);
        commentReader.validateBelongsToPost(parentComment, postId);
        commentReader.validateReplyWritable(parentComment);
        return parentComment;
    }

    private void validateDeletePermission(Long memberId, Comment comment) {
        if (comment.isAuthor(memberId) || memberService.isAdmin(memberId)) {
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
        postMetaService.decreaseCommentCount(comment.getPostId());
        if (parentId != null) {
            commentWriter.decreaseReplyCount(parentId);
            deleteParentIfRemovable(parentId);
        }
    }

    private void deleteParentIfRemovable(Long parentId) {
        commentReader.read(parentId)
                .filter(Comment::isDeleted)
                .filter(parent -> !commentReader.hasChildren(parent))
                .ifPresent(this::deletePhysically);
    }
}
