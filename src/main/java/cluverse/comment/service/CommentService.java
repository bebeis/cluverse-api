package cluverse.comment.service;

import cluverse.comment.service.request.CommentCreateRequest;
import cluverse.comment.service.request.CommentPageRequest;
import cluverse.comment.service.response.CommentDeleteResponse;
import cluverse.comment.service.response.CommentPageResponse;
import cluverse.comment.service.response.CommentResponse;

public interface CommentService {

    CommentPageResponse getComments(Long memberId, CommentPageRequest request);

    CommentResponse createComment(Long memberId, Long postId, CommentCreateRequest request, String clientIp);

    CommentDeleteResponse deleteComment(Long memberId, Long commentId);
}
