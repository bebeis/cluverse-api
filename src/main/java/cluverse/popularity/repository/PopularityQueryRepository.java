package cluverse.popularity.repository;

import cluverse.popularity.domain.PopularityAlgorithmVersion;
import cluverse.popularity.domain.PopularPostSortType;
import cluverse.popularity.repository.dto.PopularPostSummary;
import cluverse.popularity.repository.dto.PopularityPolicySample;
import cluverse.popularity.repository.dto.PopularitySnapshot;
import cluverse.post.domain.PostStatus;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static cluverse.meta.domain.QPostCommentCount.postCommentCount;
import static cluverse.meta.domain.QPostLikeCount.postLikeCount;
import static cluverse.meta.domain.QPostViewCount.postViewCount;
import static cluverse.popularity.domain.QPopularPost.popularPost;
import static cluverse.post.domain.QPost.post;

@Repository
@RequiredArgsConstructor
public class PopularityQueryRepository {

    private final JPAQueryFactory queryFactory;

    public Optional<PopularitySnapshot> findSnapshot(Long postId) {
        return Optional.ofNullable(snapshotQuery()
                .where(post.id.eq(postId), post.status.eq(PostStatus.ACTIVE))
                .fetchOne());
    }

    public List<PopularitySnapshot> findSnapshots(List<Long> postIds) {
        if (postIds.isEmpty()) {
            return List.of();
        }
        return snapshotQuery()
                .where(post.id.in(postIds), post.status.eq(PostStatus.ACTIVE))
                .fetch();
    }

    public List<PopularitySnapshot> findRecentSnapshotsAfter(
            LocalDateTime createdFrom,
            LocalDateTime lastCreatedAt,
            long lastPostId,
            int limit
    ) {
        return snapshotQuery()
                .where(
                        post.createdAt.goe(createdFrom),
                        post.createdAt.gt(lastCreatedAt)
                                .or(post.createdAt.eq(lastCreatedAt).and(post.id.gt(lastPostId))),
                        post.status.eq(PostStatus.ACTIVE)
                )
                .orderBy(post.createdAt.asc(), post.id.asc())
                .limit(limit)
                .fetch();
    }

    public List<Long> findPolicyBoardIds(LocalDateTime createdFrom, LocalDateTime createdTo) {
        return queryFactory.select(post.boardId)
                .from(post)
                .where(
                        post.createdAt.goe(createdFrom),
                        post.createdAt.lt(createdTo),
                        post.status.eq(PostStatus.ACTIVE)
                )
                .distinct()
                .fetch();
    }

    public List<PopularityPolicySample> findPolicySamples(
            Long boardId,
            LocalDateTime createdFrom,
            LocalDateTime createdTo
    ) {
        return queryFactory
                .select(Projections.constructor(
                        PopularityPolicySample.class,
                        popularPost.scoreAtPromotion,
                        postLikeCount.likeCount.coalesce(0).longValue(),
                        postCommentCount.commentCount.coalesce(0).longValue(),
                        postViewCount.viewCount.coalesce(0).longValue()
                ))
                .from(post)
                .leftJoin(postLikeCount).on(postLikeCount.postId.eq(post.id))
                .leftJoin(postCommentCount).on(postCommentCount.postId.eq(post.id))
                .leftJoin(postViewCount).on(postViewCount.postId.eq(post.id))
                .leftJoin(popularPost).on(
                        popularPost.postId.eq(post.id),
                        popularPost.algorithmVersion.eq(PopularityAlgorithmVersion.V2)
                )
                .where(
                        post.boardId.eq(boardId),
                        post.createdAt.goe(createdFrom),
                        post.createdAt.lt(createdTo),
                        post.status.eq(PostStatus.ACTIVE)
                )
                .fetch();
    }

    public List<PopularPostSummary> findPopularPosts(
            PopularityAlgorithmVersion version,
            boolean finalized,
            PopularPostSortType sort,
            int limit
    ) {
        return queryFactory
                .select(Projections.constructor(
                        PopularPostSummary.class,
                        post.id,
                        post.boardId,
                        post.title,
                        popularPost.score.coalesce(popularPost.scoreAtPromotion),
                        popularPost.likeCount.coalesce(0L),
                        popularPost.commentCount.coalesce(0L),
                        popularPost.viewCount.coalesce(0L),
                        popularPost.promotedAt,
                        popularPost.finalizedAt
                ))
                .from(popularPost)
                .join(post).on(post.id.eq(popularPost.postId))
                .where(
                        popularPost.algorithmVersion.eq(version),
                        finalized ? popularPost.finalizedAt.isNotNull() : popularPost.finalizedAt.isNull(),
                        post.status.eq(PostStatus.ACTIVE)
                )
                .orderBy(resolveOrder(sort, finalized))
                .limit(limit)
                .fetch();
    }

    private com.querydsl.jpa.impl.JPAQuery<PopularitySnapshot> snapshotQuery() {
        return queryFactory
                .select(Projections.constructor(
                        PopularitySnapshot.class,
                        post.id,
                        post.boardId,
                        post.createdAt,
                        postLikeCount.likeCount.coalesce(0).longValue(),
                        postCommentCount.commentCount.coalesce(0).longValue(),
                        postViewCount.viewCount.coalesce(0).longValue()
                ))
                .from(post)
                .leftJoin(postLikeCount).on(postLikeCount.postId.eq(post.id))
                .leftJoin(postCommentCount).on(postCommentCount.postId.eq(post.id))
                .leftJoin(postViewCount).on(postViewCount.postId.eq(post.id));
    }

    private OrderSpecifier<?>[] resolveOrder(PopularPostSortType sort, boolean finalized) {
        if (finalized && sort == PopularPostSortType.SCORE) {
            return new OrderSpecifier<?>[]{popularPost.score.desc(), popularPost.postId.desc()};
        }
        return new OrderSpecifier<?>[]{popularPost.promotedAt.desc(), popularPost.postId.desc()};
    }
}
