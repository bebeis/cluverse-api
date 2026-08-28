package cluverse.post.repository;

import cluverse.post.domain.PostCategory;
import cluverse.post.domain.PostStatus;
import cluverse.post.repository.dto.LatestPostCacheEntry;
import cluverse.post.repository.dto.PostIdSliceQueryResult;
import cluverse.post.service.request.PostCursorDirection;
import cluverse.post.service.request.PostCursorSearchRequest;
import cluverse.post.service.request.PostKeywordSearchRequest;
import cluverse.post.service.request.PostPageSearchRequest;
import cluverse.post.service.request.PostSortType;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static cluverse.meta.domain.QPostViewCount.postViewCount;
import static cluverse.post.domain.QPost.post;

@Repository
@RequiredArgsConstructor
public class PostPageQueryRepository {

    private final JPAQueryFactory queryFactory;
    private final EntityManager entityManager;

    public PostIdSliceQueryResult findOffsetPageIds(PostPageSearchRequest request) {
        int size = request.sizeOrDefault();
        long offset = (long) (request.pageOrDefault() - 1) * size;
        return findOffsetPageIds(request.boardId(), request.category(), request.sortOrDefault(), offset, size);
    }

    public List<LatestPostCacheEntry> findLatestPostCacheEntries(
            Long boardId,
            PostCategory category,
            int limit
    ) {
        return queryFactory.select(Projections.constructor(
                        LatestPostCacheEntry.class,
                        post.id,
                        post.createdAt
                ))
                .from(post)
                .where(
                        activePost(),
                        boardIdEq(boardId),
                        categoryEq(category)
                )
                .orderBy(post.createdAt.desc(), post.id.desc())
                .limit(limit)
                .fetch();
    }

    private PostIdSliceQueryResult findOffsetPageIds(
            Long boardId,
            PostCategory category,
            PostSortType sortType,
            long offset,
            int size
    ) {
        // OFFSET 이동은 커버링 인덱스에서 ID로만 끝내고, 화면용 조인은 최종 ID에만 수행한다.
        JPAQuery<Long> postIdQuery = queryFactory.select(post.id).from(post);
        if (sortType == PostSortType.VIEW_COUNT) {
            postIdQuery.leftJoin(postViewCount).on(postViewCount.postId.eq(post.id));
        }

        List<Long> postIds = postIdQuery
                .where(
                        activePost(),
                        boardIdEq(boardId),
                        categoryEq(category)
                )
                .orderBy(resolveOrderSpecifiers(sortType))
                .offset(offset)
                .limit(size + 1L)
                .fetch();

        return toSlice(postIds, size);
    }

    public PostIdSliceQueryResult findCursorPageIds(PostCursorSearchRequest request) {
        int size = request.sizeOrDefault();
        boolean ascending = request.hasCursor() && request.directionOrDefault() == PostCursorDirection.PREV;

        List<Long> postIds = queryFactory.select(post.id)
                .from(post)
                .where(
                        activePost(),
                        boardIdEq(request.boardId()),
                        categoryEq(request.category()),
                        cursorAnchor(request)
                )
                .orderBy(ascending
                        ? new OrderSpecifier<?>[]{post.createdAt.asc(), post.id.asc()}
                        : new OrderSpecifier<?>[]{post.createdAt.desc(), post.id.desc()})
                .limit(size + 1L)
                .fetch();

        PostIdSliceQueryResult slice = toSlice(postIds, size);
        if (!ascending) {
            return slice;
        }
        // PREV는 인접한 최신 글을 ASC로 가져온 뒤 API 계약인 최신순으로 되돌린다.
        List<Long> reversed = new ArrayList<>(slice.postIds());
        Collections.reverse(reversed);
        return new PostIdSliceQueryResult(reversed, slice.hasNext());
    }

    public boolean existsPostsNewerThan(Long boardId, PostCategory category, LocalDateTime exclusiveEnd) {
        return queryFactory.selectOne()
                .from(post)
                .where(
                        activePost(),
                        boardIdEq(boardId),
                        categoryEq(category),
                        post.createdAt.goe(exclusiveEnd)
                )
                .fetchFirst() != null;
    }

    private BooleanExpression cursorAnchor(PostCursorSearchRequest request) {
        if (request.hasCursor()) {
            LocalDateTime createdAt = request.cursorCreatedAt();
            Long postId = request.cursorPostId();
            // created_at 동률에서도 누락·중복이 없도록 post_id까지 포함한 튜플 경계를 만든다.
            return switch (request.directionOrDefault()) {
                case NEXT -> post.createdAt.lt(createdAt)
                        .or(post.createdAt.eq(createdAt).and(post.id.lt(postId)));
                case PREV -> post.createdAt.gt(createdAt)
                        .or(post.createdAt.eq(createdAt).and(post.id.gt(postId)));
            };
        }
        if (request.isDateAnchored()) {
            return post.createdAt.lt(request.exclusiveDateEnd());
        }
        return null;
    }

    public PostIdSliceQueryResult findKeywordPageIds(PostKeywordSearchRequest request) {
        int size = request.sizeOrDefault();
        long offset = (long) (request.pageOrDefault() - 1) * size;

        List<Long> postIds = queryFactory.selectDistinct(post.id)
                .from(post)
                .where(
                        activePost(),
                        boardIdEq(request.boardId()),
                        keywordContains(request.keyword())
                )
                .orderBy(post.createdAt.desc(), post.id.desc())
                .offset(offset)
                .limit(size + 1L)
                .fetch();

        return toSlice(postIds, size);
    }

    public PostIdSliceQueryResult findAuthorPageIds(Long authorId, int page, int size) {
        long offset = (long) (page - 1) * size;

        List<Long> postIds = queryFactory.select(post.id)
                .from(post)
                .where(
                        activePost(),
                        post.memberId.eq(authorId)
                )
                .orderBy(post.createdAt.desc(), post.id.desc())
                .offset(offset)
                .limit(size + 1L)
                .fetch();

        return toSlice(postIds, size);
    }

    /**
     * 키워드 검색용 상한 카운트. LIKE/ESCAPE 처리를 네이티브 SQL로 복제하면
     * 목록 쿼리(keywordContains)와 결과가 어긋날 수 있어, 같은 술어로 id만 상한까지 조회해 센다.
     */
    public long countPostsByKeywordUpTo(PostKeywordSearchRequest request, long searchLimit) {
        return queryFactory.selectDistinct(post.id)
                .from(post)
                .where(
                        activePost(),
                        boardIdEq(request.boardId()),
                        keywordContains(request.keyword())
                )
                .limit(searchLimit)
                .fetch()
                .size();
    }

    public long countPostsUpTo(PostPageSearchRequest request, long searchLimit) {
        // LIMIT이 있는 파생 테이블로 현재 페이지 블록에 필요한 범위까지만 센다.
        String sql = "SELECT COUNT(*) FROM ("
                + " SELECT post_id FROM post"
                + " WHERE board_id = :boardId AND status = :status"
                + (request.category() == null ? "" : " AND category = :category")
                + " LIMIT :searchLimit"
                + ") capped";

        Query query = entityManager.createNativeQuery(sql)
                .setParameter("boardId", request.boardId())
                .setParameter("status", PostStatus.ACTIVE.name())
                .setParameter("searchLimit", searchLimit);
        if (request.category() != null) {
            query.setParameter("category", request.category().name());
        }
        return ((Number) query.getSingleResult()).longValue();
    }

    private PostIdSliceQueryResult toSlice(List<Long> fetchedIds, int size) {
        boolean hasNext = fetchedIds.size() > size;
        List<Long> pageIds = hasNext ? fetchedIds.subList(0, size) : fetchedIds;
        return new PostIdSliceQueryResult(pageIds, hasNext);
    }

    private BooleanExpression activePost() {
        return post.status.eq(PostStatus.ACTIVE);
    }

    private BooleanExpression boardIdEq(Long boardId) {
        return post.boardId.eq(boardId);
    }

    private BooleanExpression categoryEq(PostCategory category) {
        return category == null ? null : post.category.eq(category);
    }

    private BooleanExpression keywordContains(String keyword) {
        return post.title.containsIgnoreCase(keyword)
                .or(post.content.containsIgnoreCase(keyword))
                .or(post.tags.any().containsIgnoreCase(keyword));
    }

    private OrderSpecifier<?>[] resolveOrderSpecifiers(PostSortType sortType) {
        return switch (sortType) {
            case VIEW_COUNT -> new OrderSpecifier<?>[]{
                    postViewCount.viewCount.coalesce(0L).desc(), post.id.desc()
            };
            case LATEST -> new OrderSpecifier<?>[]{post.createdAt.desc(), post.id.desc()};
        };
    }

}
