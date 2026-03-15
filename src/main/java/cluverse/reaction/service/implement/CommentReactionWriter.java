package cluverse.reaction.service.implement;

import cluverse.common.exception.BadRequestException;
import cluverse.reaction.domain.CommentLike;
import cluverse.reaction.exception.CommentReactionExceptionMessage;
import cluverse.reaction.repository.CommentLikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class CommentReactionWriter {

    private final CommentLikeRepository commentLikeRepository;

    public void likeComment(Long memberId, Long commentId) {
        validateNotAlreadyLiked(memberId, commentId);
        commentLikeRepository.save(CommentLike.of(commentId, memberId));
    }

    public void unlikeComment(Long memberId, Long commentId) {
        CommentLike commentLike = commentLikeRepository.findByCommentIdAndMemberId(commentId, memberId)
                .orElseThrow(() -> new BadRequestException(CommentReactionExceptionMessage.COMMENT_NOT_LIKED.getMessage()));
        commentLikeRepository.delete(commentLike);
    }

    private void validateNotAlreadyLiked(Long memberId, Long commentId) {
        if (commentLikeRepository.existsByCommentIdAndMemberId(commentId, memberId)) {
            throw new BadRequestException(CommentReactionExceptionMessage.COMMENT_ALREADY_LIKED.getMessage());
        }
    }
}
