package cluverse.comment.service;

import cluverse.comment.domain.CommentStatus;
import cluverse.comment.service.implement.CommentProcessor;
import cluverse.comment.service.implement.CommentWriter;
import cluverse.comment.service.request.CommentCreateRequest;
import cluverse.comment.service.request.CommentUpdateRequest;
import cluverse.comment.service.response.CommentDeleteResponse;
import cluverse.popularity.domain.PopularityTrigger;
import cluverse.popularity.service.implement.PopularityPromotionInvoker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentWriter commentWriter;
    private final CommentProcessor commentProcessor;
    private final PopularityPromotionInvoker popularityPromotionInvoker;

    public Long createComment(Long memberId, Long postId, CommentCreateRequest request, String clientIp) {
        Long commentId = commentProcessor.createComment(memberId, postId, request, clientIp);
        popularityPromotionInvoker.tryEvaluate(postId, PopularityTrigger.COMMENT);
        return commentId;
    }

    public Long updateComment(Long memberId, Long commentId, CommentUpdateRequest request) {
        commentWriter.update(memberId, commentId, request);
        return commentId;
    }

    public CommentDeleteResponse deleteComment(Long memberId, Long commentId) {
        Long postId = commentProcessor.deleteComment(memberId, commentId);
        return CommentDeleteResponse.delete(postId, commentId, CommentStatus.DELETED);
    }
}
