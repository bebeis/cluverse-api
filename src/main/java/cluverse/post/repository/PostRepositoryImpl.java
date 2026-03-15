package cluverse.post.repository;

import cluverse.post.domain.Post;
import cluverse.post.domain.PostStatus;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static cluverse.post.domain.QPost.post;
import static cluverse.post.domain.QPostImage.postImage;

@Repository
public class PostRepositoryImpl implements PostRepositoryCustom {

    private final EntityManager entityManager;
    private final JPAQueryFactory queryFactory;

    public PostRepositoryImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
        this.queryFactory = new JPAQueryFactory(entityManager);
    }

    @Override
    public Optional<Post> findActiveByIdForUpdate(Long postId) {
        return Optional.ofNullable(
                queryFactory.selectFrom(post)
                        .where(
                                post.id.eq(postId),
                                post.status.eq(PostStatus.ACTIVE)
                        )
                        .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                        .fetchOne()
        );
    }

    @Override
    public int increaseViewCount(Long postId) {
        return executeCount(
                queryFactory.update(post)
                        .set(post.viewCount, post.viewCount.add(1))
                        .where(
                                post.id.eq(postId),
                                post.status.eq(PostStatus.ACTIVE)
                        )
                        .execute()
        );
    }

    @Override
    public int increaseLikeCount(Long postId) {
        return executeCount(
                queryFactory.update(post)
                        .set(post.likeCount, post.likeCount.add(1))
                        .where(
                                post.id.eq(postId),
                                post.status.eq(PostStatus.ACTIVE)
                        )
                        .execute()
        );
    }

    @Override
    public int decreaseLikeCount(Long postId) {
        return executeCount(
                queryFactory.update(post)
                        .set(post.likeCount, post.likeCount.subtract(1))
                        .where(
                                post.id.eq(postId),
                                post.status.eq(PostStatus.ACTIVE),
                                post.likeCount.gt(0)
                        )
                        .execute()
        );
    }

    @Override
    public int increaseBookmarkCount(Long postId) {
        return executeCount(
                queryFactory.update(post)
                        .set(post.bookmarkCount, post.bookmarkCount.add(1))
                        .where(
                                post.id.eq(postId),
                                post.status.eq(PostStatus.ACTIVE)
                        )
                        .execute()
        );
    }

    @Override
    public int decreaseBookmarkCount(Long postId) {
        return executeCount(
                queryFactory.update(post)
                        .set(post.bookmarkCount, post.bookmarkCount.subtract(1))
                        .where(
                                post.id.eq(postId),
                                post.status.eq(PostStatus.ACTIVE),
                                post.bookmarkCount.gt(0)
                        )
                        .execute()
        );
    }

    @Override
    public Optional<Post> findWithImagesById(Long postId) {
        return Optional.ofNullable(
                queryFactory.selectFrom(post)
                        .leftJoin(post.images, postImage).fetchJoin()
                        .where(post.id.eq(postId))
                        .distinct()
                        .fetchOne()
        );
    }

    @Override
    public List<Post> findAllWithImagesByIdIn(Collection<Long> postIds) {
        return queryFactory.selectFrom(post)
                .leftJoin(post.images, postImage).fetchJoin()
                .where(post.id.in(postIds))
                .distinct()
                .fetch();
    }

    private int executeCount(long count) {
        entityManager.flush();
        entityManager.clear();
        return Math.toIntExact(count);
    }
}
