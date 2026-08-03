package cluverse.comment.service.implement;

import cluverse.comment.domain.Comment;
import cluverse.comment.domain.CommentStatus;
import cluverse.comment.domain.PostCommentActivity;
import cluverse.comment.repository.CommentRepository;
import cluverse.comment.repository.PostCommentActivityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class PostCommentActivityWriter {

    private final PostCommentActivityRepository activityRepository;
    private final CommentRepository commentRepository;

    @Transactional(propagation = Propagation.MANDATORY)
    public void reflectCreated(Comment comment) {
        activityRepository.upsertLatest(comment);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void reflectDeleted(Long postId, Long deletedCommentId) {
        activityRepository.findByPostIdForUpdate(postId)
                .filter(activity -> activity.getLastCommentId().equals(deletedCommentId))
                .ifPresent(this::replaceOrRemove);
    }

    private void replaceOrRemove(PostCommentActivity activity) {
        commentRepository.flush();
        commentRepository.findFirstByPostIdAndStatusNotOrderByCreatedAtDescIdDesc(
                        activity.getPostId(), CommentStatus.DELETED
                )
                .ifPresentOrElse(
                        activity::replaceLatest,
                        () -> activityRepository.delete(activity)
                );
    }
}
