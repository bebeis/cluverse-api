package cluverse.comment.repository;

public interface CommentRepositoryCustom {

    int increaseLikeCount(Long commentId);

    int decreaseLikeCount(Long commentId);

    int increaseReplyCount(Long commentId);

    int decreaseReplyCount(Long commentId);
}
