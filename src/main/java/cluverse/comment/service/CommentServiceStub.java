package cluverse.comment.service;

import cluverse.comment.service.request.CommentCreateRequest;
import cluverse.comment.service.request.CommentPageRequest;
import cluverse.comment.service.response.CommentDeleteResponse;
import cluverse.comment.service.response.CommentPageResponse;
import cluverse.comment.service.response.CommentResponse;
import org.springframework.stereotype.Service;

@Service
public class CommentServiceStub implements CommentService {

    private static final String MESSAGE = "댓글 API는 현재 설계만 완료되었고, 서비스 구현은 아직 추가되지 않았습니다.";

    @Override
    public CommentPageResponse getComments(Long memberId, CommentPageRequest request) {
        throw new UnsupportedOperationException(MESSAGE);
    }

    @Override
    public CommentResponse createComment(Long memberId, Long postId, CommentCreateRequest request, String clientIp) {
        throw new UnsupportedOperationException(MESSAGE);
    }

    @Override
    public CommentDeleteResponse deleteComment(Long memberId, Long commentId) {
        throw new UnsupportedOperationException(MESSAGE);
    }
}
