package cluverse.post.repository;

import cluverse.post.domain.Post;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static cluverse.post.domain.QPost.post;
import static cluverse.post.domain.QPostImage.postImage;

@Repository
public class PostRepositoryImpl implements PostRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    public PostRepositoryImpl(EntityManager entityManager) {
        this.queryFactory = new JPAQueryFactory(entityManager);
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
}
