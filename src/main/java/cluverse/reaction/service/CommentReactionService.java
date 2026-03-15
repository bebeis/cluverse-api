package cluverse.reaction.service;

import cluverse.reaction.service.response.CommentLikeResponse;
import org.springframework.stereotype.Service;

@Service
public class CommentReactionService {

    private static final String MESSAGE = "댓글 반응 API는 현재 설계만 완료되었고, 서비스 구현은 아직 추가되지 않았습니다.";

    public CommentLikeResponse likeComment(Long memberId, Long commentId) {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public CommentLikeResponse unlikeComment(Long memberId, Long commentId) {
        throw new UnsupportedOperationException(MESSAGE);
    }
}
