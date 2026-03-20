package cluverse.comment.service;

import cluverse.comment.service.request.CommentCreateRequest;
import cluverse.comment.service.request.CommentPageRequest;
import cluverse.comment.service.request.CommentUpdateRequest;
import cluverse.comment.service.response.CommentDeleteResponse;
import cluverse.comment.service.response.CommentPageResponse;
import cluverse.comment.service.response.CommentReactionTargetResponse;
import cluverse.comment.service.response.CommentResponse;

public interface CommentService {

    CommentPageResponse getComments(Long memberId, CommentPageRequest request);

    CommentResponse createComment(Long memberId, Long postId, CommentCreateRequest request, String clientIp);

    CommentResponse updateComment(Long memberId, Long commentId, CommentUpdateRequest request);

    CommentDeleteResponse deleteComment(Long memberId, Long commentId);

    CommentReactionTargetResponse getReactionTarget(Long commentId);

    void increaseLikeCount(Long commentId);

    void decreaseLikeCount(Long commentId);
}
