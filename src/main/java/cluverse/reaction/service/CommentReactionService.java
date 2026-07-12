package cluverse.reaction.service;

import cluverse.comment.service.response.CommentReactionTargetResponse;
import cluverse.reaction.service.implement.CommentReactionProcessor;
import cluverse.reaction.service.response.CommentLikeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CommentReactionService {

    private final CommentReactionProcessor commentReactionProcessor;

    public CommentLikeResponse likeComment(Long memberId, Long commentId) {
        CommentReactionTargetResponse target = commentReactionProcessor.likeComment(memberId, commentId);
        return CommentLikeResponse.like(target.postId(), target.commentId());
    }

    public CommentLikeResponse unlikeComment(Long memberId, Long commentId) {
        CommentReactionTargetResponse target = commentReactionProcessor.unlikeComment(memberId, commentId);
        return CommentLikeResponse.unlike(target.postId(), target.commentId());
    }
}
