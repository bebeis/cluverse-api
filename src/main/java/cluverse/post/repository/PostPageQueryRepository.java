package cluverse.post.repository;

import cluverse.post.domain.PostCategory;
import cluverse.post.domain.PostStatus;
import cluverse.post.repository.dto.PostIdSliceQueryResult;
import cluverse.post.service.request.PostCursorDirection;
import cluverse.post.service.request.PostCursorSearchRequest;
import cluverse.post.service.request.PostKeywordSearchRequest;
import cluverse.post.service.request.PostOffsetSearchRequest;
import cluverse.post.service.request.PostSearchRequest;
import cluverse.post.service.request.PostSortType;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static cluverse.meta.domain.QPostViewCount.postViewCount;
import static cluverse.post.domain.QPost.post;

/**
 * 페이지에 실을 게시글 id 선정과 페이지 블록용 상한 카운트만 담당한다.
 * 선정된 id로 화면 데이터를 프로젝션하는 것은 {@link PostQueryRepository},
 * 둘을 조립하는 것은 PostReader의 몫이다.
 */
@Repository
@RequiredArgsConstructor
public class PostPageQueryRepository {

    private final JPAQueryFactory queryFactory;
    private final EntityManager entityManager;

    public PostIdSliceQueryResult findPostPageIds(PostSearchRequest request) {
        int size = request.sizeOrDefault();
        long offset = (long) (request.pageOrDefault() - 1) * size;
        return findPostPageIds(request.boardId(), request.category(), request.sortOrDefault(), offset, size);
    }

    /**
     * [V2 전용] V3와 같은 커버링 인덱스 id 선정이지만, 측정용으로 페이지 상한이 완화된 요청을 받는다.
     */
    public PostIdSliceQueryResult findPostPageIds(PostOffsetSearchRequest request) {
        return findPostPageIds(
                request.boardId(), request.category(), request.sortOrDefault(),
                request.offset(), request.sizeOrDefault()
        );
    }

    private PostIdSliceQueryResult findPostPageIds(Long boardId, PostCategory category, PostSortType sortType,
                                                   long offset, int size) {
        // LATEST 정렬 시 조인 없이 idx_post_board_status_created_id 커버링 인덱스만으로
        // offset 이동이 처리되어야 한다. MySQL은 미사용 LEFT JOIN을 제거하지 않으므로
        // post_view_count 조인은 VIEW_COUNT 정렬일 때만 붙인다. (동적 쿼리 형태로 만들기!)
        JPAQuery<Long> postIdQuery = queryFactory.select(post.id)
                .from(post);
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

    /**
     * [V4 전용] 날짜 앵커/커서 기반 id 선정. offset 없이 인덱스에서 시작 지점을 바로 찾는다.
     * - 진입(date): created_at < date+1일 — (created_at, post_id) <= (그날 마지막, MAX)와 동치
     * - NEXT(과거로): (created_at, post_id) < 커서 — OR 전개형(QueryDSL은 row constructor 미지원)
     * - PREV(최신으로): (created_at, post_id) > 커서 — ASC로 커서에 인접한 size건을 뽑은 뒤 최신순으로 뒤집는다
     */
    public PostIdSliceQueryResult findPostPageIdsByCursor(PostCursorSearchRequest request) {
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
        List<Long> reversed = new ArrayList<>(slice.postIds());
        Collections.reverse(reversed);
        return new PostIdSliceQueryResult(reversed, slice.hasNext());
    }

    /**
     * [V4 전용] date 진입 페이지의 hasPrev(더 최신 글 존재) 판단.
     */
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

    public PostIdSliceQueryResult findPostPageIdsByDate(PostSearchRequest request) {
        int size = request.sizeOrDefault();
        LocalDate date = request.date();

        List<Long> postIds = queryFactory.select(post.id)
                .from(post)
                .where(
                        activePost(),
                        boardIdEq(request.boardId()),
                        categoryEq(request.category()),
                        post.createdAt.goe(date.atStartOfDay()),
                        post.createdAt.lt(date.plusDays(1).atTime(LocalTime.MIDNIGHT))
                )
                .orderBy(post.createdAt.desc(), post.id.desc())
                .limit(size + 1L)
                .fetch();

        return toSlice(postIds, size);
    }

    public PostIdSliceQueryResult findPostPageIdsByKeyword(PostKeywordSearchRequest request) {
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

    public PostIdSliceQueryResult findPostPageIdsByAuthor(Long authorId, int page, int size) {
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
     * [V1/V2 전용] 상한 없는 전체 카운트. 조건에 맞는 인덱스 엔트리를 전부 세므로
     * 게시글 수에 비례해 느려진다 — V3의 {@link #countPostsUpTo}가 이를 개선한 형태다.
     */
    public long countPosts(Long boardId, PostCategory category) {
        Long count = queryFactory.select(post.count())
                .from(post)
                .where(
                        activePost(),
                        boardIdEq(boardId),
                        categoryEq(category)
                )
                .fetchOne();
        return count == null ? 0L : count;
    }

    /**
     * 페이지 블록 렌더링에 필요한 만큼만 세는 카운트 쿼리.
     * 파생 테이블의 LIMIT 덕분에 스캔이 searchLimit에서 멈추고,
     * LIMIT을 포함한 파생 테이블은 derived merge 대상에서 제외되므로 이 형태를 유지해야 함!
     */
    public long countPostsUpTo(PostSearchRequest request, long searchLimit) {
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
            case VIEW_COUNT -> new OrderSpecifier<?>[]{postViewCount.viewCount.coalesce(0).desc(), post.id.desc()};
            case LATEST -> new OrderSpecifier<?>[]{post.createdAt.desc(), post.id.desc()};
        };
    }
}
