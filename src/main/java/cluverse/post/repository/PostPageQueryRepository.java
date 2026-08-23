package cluverse.post.repository;

import cluverse.post.domain.PostCategory;
import cluverse.post.domain.PostStatus;
import cluverse.post.repository.dto.PostIdSliceQueryResult;
import cluverse.post.service.request.PostCursorDirection;
import cluverse.post.service.request.PostCursorSearchRequest;
import cluverse.post.service.request.PostKeywordSearchRequest;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static cluverse.post.domain.QPost.post;

@Repository
@RequiredArgsConstructor
public class PostPageQueryRepository {

    private final JPAQueryFactory queryFactory;
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

}
