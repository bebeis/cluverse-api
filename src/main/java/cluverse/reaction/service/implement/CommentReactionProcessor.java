package cluverse.reaction.service.implement;

import cluverse.comment.service.implement.CommentReader;
import cluverse.comment.service.implement.CommentWriter;
import cluverse.comment.service.response.CommentReactionTargetResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class CommentReactionProcessor {

    private final CommentReactionWriter commentReactionWriter;
    private final CommentReader commentReader;
    private final CommentWriter commentWriter;

    public CommentReactionTargetResponse likeComment(Long memberId, Long commentId) {
        CommentReactionTargetResponse target = commentReader.readReactionTarget(commentId);
        commentReactionWriter.likeComment(memberId, commentId);
        commentWriter.increaseLikeCount(commentId);
        return target;
    }

    public CommentReactionTargetResponse unlikeComment(Long memberId, Long commentId) {
        CommentReactionTargetResponse target = commentReader.readReactionTarget(commentId);
        commentReactionWriter.unlikeComment(memberId, commentId);
        commentWriter.decreaseLikeCount(commentId);
        return target;
    }
}
