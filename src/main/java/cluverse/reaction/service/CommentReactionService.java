package cluverse.reaction.service;

import cluverse.comment.service.response.CommentReactionTargetResponse;
import cluverse.comment.service.implement.CommentReader;
import cluverse.comment.service.implement.CommentWriter;
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
    private final CommentReader commentReader;
    private final CommentWriter commentWriter;

    public CommentLikeResponse likeComment(Long memberId, Long commentId) {
        CommentReactionTargetResponse target = commentReader.readReactionTarget(commentId);
        commentReactionWriter.likeComment(memberId, commentId);
        commentWriter.increaseLikeCount(commentId);
        return CommentLikeResponse.like(target.postId(), target.commentId());
    }

    public CommentLikeResponse unlikeComment(Long memberId, Long commentId) {
        CommentReactionTargetResponse target = commentReader.readReactionTarget(commentId);
        commentReactionWriter.unlikeComment(memberId, commentId);
        commentWriter.decreaseLikeCount(commentId);
        return CommentLikeResponse.unlike(target.postId(), target.commentId());
    }
}
