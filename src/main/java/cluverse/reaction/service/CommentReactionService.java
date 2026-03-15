package cluverse.reaction.service;

import cluverse.comment.service.CommentService;
import cluverse.comment.service.response.CommentReactionTargetResponse;
import cluverse.reaction.service.implement.CommentReactionWriter;
import cluverse.reaction.service.response.CommentLikeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CommentReactionService {

    private final CommentReactionWriter commentReactionWriter;
    private final CommentService commentService;

    public CommentLikeResponse likeComment(Long memberId, Long commentId) {
        CommentReactionTargetResponse target = commentService.getReactionTarget(commentId);
        commentReactionWriter.likeComment(memberId, commentId);
        commentService.increaseLikeCount(commentId);
        return CommentLikeResponse.like(target.postId(), target.commentId());
    }

    public CommentLikeResponse unlikeComment(Long memberId, Long commentId) {
        CommentReactionTargetResponse target = commentService.getReactionTarget(commentId);
        commentReactionWriter.unlikeComment(memberId, commentId);
        commentService.decreaseLikeCount(commentId);
        return CommentLikeResponse.unlike(target.postId(), target.commentId());
    }
}
