package cluverse.comment.repository;

import cluverse.comment.domain.CommentStatus;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import static cluverse.comment.domain.QComment.comment;

@Repository
public class CommentRepositoryImpl implements CommentRepositoryCustom {

    private final EntityManager entityManager;
    private final JPAQueryFactory queryFactory;

    public CommentRepositoryImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
        this.queryFactory = new JPAQueryFactory(entityManager);
    }

    @Override
    public int increaseLikeCount(Long commentId) {
        return executeCount(
                queryFactory.update(comment)
                        .set(comment.likeCount, comment.likeCount.add(1))
                        .where(
                                comment.id.eq(commentId),
                                comment.status.eq(CommentStatus.ACTIVE)
                        )
                        .execute()
        );
    }

    @Override
    public int decreaseLikeCount(Long commentId) {
        return executeCount(
                queryFactory.update(comment)
                        .set(comment.likeCount, comment.likeCount.subtract(1))
                        .where(
                                comment.id.eq(commentId),
                                comment.likeCount.gt(0)
                        )
                        .execute()
        );
    }

    @Override
    public int increaseReplyCount(Long commentId) {
        return executeCount(
                queryFactory.update(comment)
                        .set(comment.replyCount, comment.replyCount.add(1))
                        .where(
                                comment.id.eq(commentId),
                                comment.status.eq(CommentStatus.ACTIVE)
                        )
                        .execute()
        );
    }

    @Override
    public int decreaseReplyCount(Long commentId) {
        return executeCount(
                queryFactory.update(comment)
                        .set(comment.replyCount, comment.replyCount.subtract(1))
                        .where(
                                comment.id.eq(commentId),
                                comment.replyCount.gt(0)
                        )
                        .execute()
        );
    }

    private int executeCount(long count) {
        entityManager.flush();
        entityManager.clear();
        return Math.toIntExact(count);
    }
}
