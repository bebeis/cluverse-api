package cluverse.post.repository;

import cluverse.common.exception.NotFoundException;
import cluverse.post.domain.PostStatus;
import cluverse.post.domain.QPostImage;
import cluverse.post.exception.PostExceptionMessage;
import cluverse.post.repository.dto.PostDetailQueryDto;
import cluverse.post.repository.dto.PostPageQueryResult;
import cluverse.post.repository.dto.PostSummaryQueryDto;
import cluverse.post.service.request.PostSearchRequest;
import cluverse.post.service.request.PostSortType;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.BooleanPath;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.types.dsl.StringExpression;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import static cluverse.member.domain.QMember.member;
import static cluverse.member.domain.QMemberProfile.memberProfile;
import static cluverse.meta.domain.QPostBookmarkCount.postBookmarkCount;
import static cluverse.meta.domain.QPostCommentCount.postCommentCount;
import static cluverse.meta.domain.QPostLikeCount.postLikeCount;
import static cluverse.meta.domain.QPostViewCount.postViewCount;
import static cluverse.board.domain.QBoard.board;
import static cluverse.post.domain.QPost.post;
import static cluverse.post.domain.QPostImage.postImage;

@Repository
@RequiredArgsConstructor
public class PostQueryRepository {

    private static final QPostImage THUMBNAIL_IMAGE = new QPostImage("thumbnailImage");
    private static final StringPath POST_TAG = Expressions.stringPath("postTag");
    private static final BooleanPath IS_MINE = Expressions.booleanPath("isMine");
    private static final StringPath CONTENT_PREVIEW = Expressions.stringPath("contentPreview");
    private static final NumberPath<Long> LIKE_COUNT = Expressions.numberPath(Long.class, "likeCount");
    private static final NumberPath<Long> COMMENT_COUNT = Expressions.numberPath(Long.class, "commentCount");
    private static final NumberPath<Long> BOOKMARK_COUNT = Expressions.numberPath(Long.class, "bookmarkCount");
    private static final NumberPath<Long> VIEW_COUNT = Expressions.numberPath(Long.class, "viewCount");
    private static final int CONTENT_PREVIEW_LENGTH = 120;

    private final JPAQueryFactory queryFactory;

    public PostPageQueryResult findPostPage(Long memberId, PostSearchRequest request) {
        int size = request.sizeOrDefault();
        long offset = (long) (request.pageOrDefault() - 1) * size;

        List<Long> postIds = queryFactory.select(post.id)
                .from(post)
                .leftJoin(postViewCount).on(postViewCount.postId.eq(post.id))
                .where(
                        post.status.eq(PostStatus.ACTIVE),
                        boardIdEq(request.boardId()),
                        categoryEq(request.category())
                )
                .orderBy(resolveOrderSpecifiers(request.sortOrDefault()))
                .offset(offset)
                .limit(size + 1L)
                .fetch();

        boolean hasNext = postIds.size() > size;
        if (hasNext) {
            postIds = postIds.subList(0, size);
        }
        return new PostPageQueryResult(readPostSummaries(memberId, postIds), hasNext);
    }

    public PostPageQueryResult findPostPageByDate(Long memberId, PostSearchRequest request) {
        int size = request.sizeOrDefault();
        LocalDate date = request.date();

        List<Long> postIds = queryFactory.select(post.id)
                .from(post)
                .where(
                        post.status.eq(PostStatus.ACTIVE),
                        boardIdEq(request.boardId()),
                        categoryEq(request.category()),
                        post.createdAt.goe(date.atStartOfDay()),
                        post.createdAt.lt(date.plusDays(1).atTime(LocalTime.MIDNIGHT))
                )
                .orderBy(post.createdAt.desc(), post.id.desc())
                .limit(size + 1L)
                .fetch();

        boolean hasNext = postIds.size() > size;
        if (hasNext) {
            postIds = postIds.subList(0, size);
        }
        return new PostPageQueryResult(readPostSummaries(memberId, postIds), hasNext);
    }

    public PostDetailQueryDto findPostDetail(Long memberId, Long postId) {
        Expression<Boolean> isMineExpression = isMineExpression(memberId);

        List<Tuple> rows = queryFactory
                .select(
                        post.id,
                        post.boardId,
                        board.boardType,
                        board.name,
                        board.parentId,
                        post.category,
                        post.title,
                        post.content,
                        POST_TAG,
                        postImage.imageUrl,
                        post.isAnonymous,
                        post.isPinned,
                        post.isExternalVisible,
                        isMineExpression,
                        ExpressionUtils.as(postViewCount.viewCount.coalesce(0).longValue(), VIEW_COUNT),
                        ExpressionUtils.as(postLikeCount.likeCount.coalesce(0).longValue(), LIKE_COUNT),
                        ExpressionUtils.as(postCommentCount.commentCount.coalesce(0).longValue(), COMMENT_COUNT),
                        ExpressionUtils.as(postBookmarkCount.bookmarkCount.coalesce(0).longValue(), BOOKMARK_COUNT),
                        member.id,
                        member.nickname,
                        memberProfile.profileImageUrl,
                        post.createdAt,
                        post.updatedAt
                )
                .from(post)
                .leftJoin(post.tags, POST_TAG)
                .leftJoin(postImage).on(postImage.post.eq(post))
                .leftJoin(postLikeCount).on(postLikeCount.postId.eq(post.id))
                .leftJoin(postCommentCount).on(postCommentCount.postId.eq(post.id))
                .leftJoin(postBookmarkCount).on(postBookmarkCount.postId.eq(post.id))
                .leftJoin(postViewCount).on(postViewCount.postId.eq(post.id))
                .join(board).on(board.id.eq(post.boardId))
                .join(member).on(member.id.eq(post.memberId))
                .leftJoin(memberProfile).on(memberProfile.memberId.eq(member.id))
                .where(
                        post.status.eq(PostStatus.ACTIVE),
                        post.id.eq(postId)
                )
                .orderBy(postImage.displayOrder.asc())
                .fetch()
                ;

        if (rows.isEmpty()) {
            throw new NotFoundException(PostExceptionMessage.POST_NOT_FOUND.getMessage());
        }

        return toPostDetailQueryDto(rows);
    }

    private BooleanExpression boardIdEq(Long boardId) {
        return post.boardId.eq(boardId);
    }

    private BooleanExpression categoryEq(cluverse.post.domain.PostCategory category) {
        return category == null ? null : post.category.eq(category);
    }

    private OrderSpecifier<?>[] resolveOrderSpecifiers(PostSortType sortType) {
        return switch (sortType) {
            case VIEW_COUNT -> new OrderSpecifier<?>[]{postViewCount.viewCount.coalesce(0).desc(), post.id.desc()};
            case LATEST -> new OrderSpecifier<?>[]{post.createdAt.desc(), post.id.desc()};
        };
    }

    private List<PostSummaryQueryDto> readPostSummaries(Long memberId, List<Long> postIds) {
        if (postIds.isEmpty()) {
            return List.of();
        }

        Expression<Boolean> isMineExpression = isMineExpression(memberId);
        Expression<String> contentPreviewExpression = contentPreviewExpression();

        List<Tuple> rows = queryFactory
                .select(
                        post.id,
                        post.boardId,
                        post.category,
                        post.title,
                        contentPreviewExpression,
                        POST_TAG,
                        THUMBNAIL_IMAGE.imageUrl,
                        post.isAnonymous,
                        post.isPinned,
                        post.isExternalVisible,
                        isMineExpression,
                        ExpressionUtils.as(postViewCount.viewCount.coalesce(0).longValue(), VIEW_COUNT),
                        ExpressionUtils.as(postLikeCount.likeCount.coalesce(0).longValue(), LIKE_COUNT),
                        ExpressionUtils.as(postCommentCount.commentCount.coalesce(0).longValue(), COMMENT_COUNT),
                        ExpressionUtils.as(postBookmarkCount.bookmarkCount.coalesce(0).longValue(), BOOKMARK_COUNT),
                        member.id,
                        member.nickname,
                        memberProfile.profileImageUrl,
                        post.createdAt
                )
                .from(post)
                .leftJoin(post.tags, POST_TAG)
                .leftJoin(THUMBNAIL_IMAGE).on(
                        THUMBNAIL_IMAGE.post.eq(post),
                        THUMBNAIL_IMAGE.displayOrder.eq(0)
                )
                .leftJoin(postLikeCount).on(postLikeCount.postId.eq(post.id))
                .leftJoin(postCommentCount).on(postCommentCount.postId.eq(post.id))
                .leftJoin(postBookmarkCount).on(postBookmarkCount.postId.eq(post.id))
                .leftJoin(postViewCount).on(postViewCount.postId.eq(post.id))
                .join(member).on(member.id.eq(post.memberId))
                .leftJoin(memberProfile).on(memberProfile.memberId.eq(member.id))
                .where(post.id.in(postIds))
                .fetch();

        Map<Long, Tuple> rowByPostId = new LinkedHashMap<>();
        Map<Long, LinkedHashSet<String>> tagsByPostId = new LinkedHashMap<>();
        for (Tuple row : rows) {
            Long postId = row.get(post.id);
            rowByPostId.putIfAbsent(postId, row);
            String tag = row.get(POST_TAG);
            if (tag != null) {
                tagsByPostId.computeIfAbsent(postId, ignored -> new LinkedHashSet<>()).add(tag);
            }
        }

        return postIds.stream()
                .map(postId -> toPostSummaryQueryDto(
                        rowByPostId.get(postId),
                        new ArrayList<>(tagsByPostId.getOrDefault(postId, new LinkedHashSet<>()))
                ))
                .toList();
    }

    private PostSummaryQueryDto toPostSummaryQueryDto(Tuple row, List<String> tags) {
        return new PostSummaryQueryDto(
                row.get(post.id),
                row.get(post.boardId),
                row.get(post.category),
                row.get(post.title),
                row.get(CONTENT_PREVIEW),
                tags,
                row.get(THUMBNAIL_IMAGE.imageUrl),
                booleanValue(row.get(post.isAnonymous)),
                booleanValue(row.get(post.isPinned)),
                booleanValue(row.get(post.isExternalVisible)),
                booleanValue(row.get(IS_MINE)),
                numberValue(row.get(VIEW_COUNT)),
                numberValue(row.get(LIKE_COUNT)),
                numberValue(row.get(COMMENT_COUNT)),
                numberValue(row.get(BOOKMARK_COUNT)),
                row.get(member.id),
                row.get(member.nickname),
                row.get(memberProfile.profileImageUrl),
                row.get(post.createdAt)
        );
    }

    private PostDetailQueryDto toPostDetailQueryDto(List<Tuple> rows) {
        Tuple firstRow = rows.getFirst();
        LinkedHashSet<String> tags = new LinkedHashSet<>();
        LinkedHashSet<String> imageUrls = new LinkedHashSet<>();

        for (Tuple row : rows) {
            String tag = row.get(POST_TAG);
            if (tag != null) {
                tags.add(tag);
            }

            String imageUrl = row.get(postImage.imageUrl);
            if (imageUrl != null) {
                imageUrls.add(imageUrl);
            }
        }

        return new PostDetailQueryDto(
                firstRow.get(post.id),
                firstRow.get(post.boardId),
                firstRow.get(board.boardType),
                firstRow.get(board.name),
                firstRow.get(board.parentId),
                firstRow.get(post.category),
                firstRow.get(post.title),
                firstRow.get(post.content),
                new ArrayList<>(tags),
                new ArrayList<>(imageUrls),
                booleanValue(firstRow.get(post.isAnonymous)),
                booleanValue(firstRow.get(post.isPinned)),
                booleanValue(firstRow.get(post.isExternalVisible)),
                booleanValue(firstRow.get(IS_MINE)),
                numberValue(firstRow.get(VIEW_COUNT)),
                numberValue(firstRow.get(LIKE_COUNT)),
                numberValue(firstRow.get(COMMENT_COUNT)),
                numberValue(firstRow.get(BOOKMARK_COUNT)),
                firstRow.get(member.id),
                firstRow.get(member.nickname),
                firstRow.get(memberProfile.profileImageUrl),
                firstRow.get(post.createdAt),
                firstRow.get(post.updatedAt)
        );
    }

    private Expression<Boolean> isMineExpression(Long memberId) {
        if (memberId == null) {
            return ExpressionUtils.as(Expressions.asBoolean(false), IS_MINE);
        }
        return ExpressionUtils.as(post.memberId.eq(memberId), IS_MINE);
    }

    private Expression<String> contentPreviewExpression() {
        StringExpression contentPreview = Expressions.stringTemplate(
                "substring({0}, 1, {1})",
                post.content,
                Expressions.constant(CONTENT_PREVIEW_LENGTH)
        );
        return ExpressionUtils.as(contentPreview, CONTENT_PREVIEW);
    }

    private boolean booleanValue(Boolean value) {
        return Boolean.TRUE.equals(value);
    }

    private long numberValue(Number value) {
        return value == null ? 0L : value.longValue();
    }
}
