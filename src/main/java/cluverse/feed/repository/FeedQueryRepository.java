package cluverse.feed.repository;

import cluverse.board.domain.BoardType;
import cluverse.feed.repository.dto.FeedPageQueryResult;
import cluverse.feed.repository.dto.FeedPostQueryDto;
import cluverse.feed.service.request.FollowingFeedScope;
import cluverse.feed.service.request.HomeFeedFilter;
import cluverse.post.domain.PostCategory;
import cluverse.post.domain.PostStatus;
import cluverse.post.domain.QPostImage;
import cluverse.member.domain.MemberStatus;
import cluverse.reaction.domain.QPostBookmark;
import cluverse.reaction.domain.QPostLike;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.BooleanPath;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.types.dsl.StringExpression;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cluverse.board.domain.QBoard.board;
import static cluverse.group.domain.QGroup.group;
import static cluverse.group.domain.QGroupMember.groupMember;
import static cluverse.interest.domain.QInterest.interest;
import static cluverse.major.domain.QMajor.major;
import static cluverse.member.domain.QBlock.block;
import static cluverse.member.domain.QFollow.follow;
import static cluverse.member.domain.QMember.member;
import static cluverse.member.domain.QMemberMajor.memberMajor;
import static cluverse.member.domain.QMemberProfile.memberProfile;
import static cluverse.meta.domain.QPostBookmarkCount.postBookmarkCount;
import static cluverse.meta.domain.QPostCommentCount.postCommentCount;
import static cluverse.meta.domain.QPostLikeCount.postLikeCount;
import static cluverse.meta.domain.QPostViewCount.postViewCount;
import static cluverse.post.domain.QPost.post;

@Repository
@RequiredArgsConstructor
public class FeedQueryRepository {

    private static final long UNKNOWN_MEMBER_ID = -1L;
    private static final String CURSOR_DELIMITER = "|";
    private static final int CONTENT_PREVIEW_LENGTH = 120;
    private static final QPostImage THUMBNAIL_IMAGE = new QPostImage("thumbnailImage");
    private static final QPostLike MEMBER_POST_LIKE = new QPostLike("memberPostLike");
    private static final QPostBookmark MEMBER_POST_BOOKMARK = new QPostBookmark("memberPostBookmark");
    private static final StringPath POST_TAG = Expressions.stringPath("postTag");
    private static final BooleanPath IS_MINE = Expressions.booleanPath("isMine");
    private static final BooleanPath LIKED = Expressions.booleanPath("liked");
    private static final BooleanPath BOOKMARKED = Expressions.booleanPath("bookmarked");
    private static final StringPath CONTENT_PREVIEW = Expressions.stringPath("contentPreview");
    private static final NumberPath<Long> LIKE_COUNT = Expressions.numberPath(Long.class, "likeCount");
    private static final NumberPath<Long> COMMENT_COUNT = Expressions.numberPath(Long.class, "commentCount");
    private static final NumberPath<Long> BOOKMARK_COUNT = Expressions.numberPath(Long.class, "bookmarkCount");
    private static final NumberPath<Long> VIEW_COUNT = Expressions.numberPath(Long.class, "viewCount");
    private static final NumberPath<Long> TRENDING_SCORE = Expressions.numberPath(Long.class, "trendingScore");

    private final JPAQueryFactory queryFactory;

    public Long findUniversityId(Long memberId) {
        if (memberId == null) {
            return null;
        }
        return queryFactory.select(member.universityId)
                .from(member)
                .where(member.id.eq(memberId))
                .fetchOne();
    }

    public Set<Long> findSubscribedBoardIds(Long memberId) {
        if (memberId == null) {
            return Set.of();
        }

        LinkedHashSet<Long> subscribedBoardIds = new LinkedHashSet<>();
        subscribedBoardIds.addAll(readMajorBoardIds(memberId));
        subscribedBoardIds.addAll(readInterestBoardIds(memberId));
        return Set.copyOf(subscribedBoardIds);
    }

    public Set<Long> findFollowingMemberIds(Long memberId) {
        if (memberId == null) {
            return Set.of();
        }
        return Set.copyOf(queryFactory.select(follow.followingId)
                .from(follow)
                .where(follow.followerId.eq(memberId))
                .fetch());
    }

    public Set<Long> findMyGroupBoardIds(Long memberId) {
        if (memberId == null) {
            return Set.of();
        }
        return Set.copyOf(queryFactory.select(group.boardId)
                .from(groupMember)
                .join(groupMember.group, group)
                .where(groupMember.memberId.eq(memberId))
                .fetch());
    }

    public Set<Long> findBlockedMemberIds(Long memberId) {
        if (memberId == null) {
            return Set.of();
        }
        return Set.copyOf(queryFactory.select(block.blockedId)
                .from(block)
                .where(block.blockerId.eq(memberId))
                .fetch());
    }

    public FeedPageQueryResult findHomeFeed(Long memberId,
                                            HomeFeedFilter filter,
                                            Long universityId,
                                            Collection<Long> subscribedBoardIds,
                                            Collection<Long> blockedMemberIds,
                                            Collection<Long> readableGroupBoardIds,
                                            LocalDateTime cursorCreatedAt,
                                            Long cursorPostId,
                                            int limit) {
        BooleanExpression personalizedCondition = resolveHomeSourceCondition(filter, universityId, subscribedBoardIds);
        return findLatestFeed(
                memberId,
                personalizedCondition,
                blockedMemberIds,
                readableGroupBoardIds,
                cursorCreatedAt,
                cursorPostId,
                limit
        );
    }

    public FeedPageQueryResult findFollowingFeed(Long memberId,
                                                 FollowingFeedScope scope,
                                                 Collection<Long> followingMemberIds,
                                                 Collection<Long> myGroupBoardIds,
                                                 Collection<Long> blockedMemberIds,
                                                 LocalDateTime cursorCreatedAt,
                                                 Long cursorPostId,
                                                 int limit) {
        BooleanExpression sourceCondition = resolveFollowingSourceCondition(scope, followingMemberIds, myGroupBoardIds);
        return findLatestFeed(
                memberId,
                sourceCondition,
                blockedMemberIds,
                myGroupBoardIds,
                cursorCreatedAt,
                cursorPostId,
                limit
        );
    }

    public FeedPageQueryResult findTrendingFeed(Long memberId,
                                                LocalDateTime since,
                                                PostCategory category,
                                                Collection<Long> blockedMemberIds,
                                                Collection<Long> readableGroupBoardIds,
                                                Long cursorScore,
                                                LocalDateTime cursorCreatedAt,
                                                Long cursorPostId,
                                                int limit) {
        NumberExpression<Long> scoreExpression = trendingScoreExpression();

        List<Tuple> candidateRows = queryFactory
                .select(post.id, ExpressionUtils.as(scoreExpression, TRENDING_SCORE), post.createdAt)
                .from(post)
                .join(board).on(board.id.eq(post.boardId))
                .join(member).on(member.id.eq(post.memberId))
                .leftJoin(postLikeCount).on(postLikeCount.postId.eq(post.id))
                .leftJoin(postCommentCount).on(postCommentCount.postId.eq(post.id))
                .leftJoin(postBookmarkCount).on(postBookmarkCount.postId.eq(post.id))
                .leftJoin(postViewCount).on(postViewCount.postId.eq(post.id))
                .where(
                        basePostCondition(blockedMemberIds, readableGroupBoardIds),
                        categoryEq(category),
                        post.createdAt.goe(since),
                        trendingCursorCondition(scoreExpression, cursorScore, cursorCreatedAt, cursorPostId)
                )
                .orderBy(scoreExpression.desc(), post.createdAt.desc(), post.id.desc())
                .limit(limit + 1L)
                .fetch();

        return toTrendingFeedPageQueryResult(memberId, candidateRows, limit);
    }

    private FeedPageQueryResult findLatestFeed(Long memberId,
                                               BooleanExpression sourceCondition,
                                               Collection<Long> blockedMemberIds,
                                               Collection<Long> readableGroupBoardIds,
                                               LocalDateTime cursorCreatedAt,
                                               Long cursorPostId,
                                               int limit) {
        List<Tuple> candidateRows = queryFactory
                .select(post.id, post.createdAt)
                .from(post)
                .join(board).on(board.id.eq(post.boardId))
                .join(member).on(member.id.eq(post.memberId))
                .where(
                        basePostCondition(blockedMemberIds, readableGroupBoardIds),
                        sourceCondition,
                        latestCursorCondition(cursorCreatedAt, cursorPostId)
                )
                .orderBy(post.createdAt.desc(), post.id.desc())
                .limit(limit + 1L)
                .fetch();

        return toLatestFeedPageQueryResult(memberId, candidateRows, limit);
    }

    private FeedPageQueryResult toLatestFeedPageQueryResult(Long memberId, List<Tuple> candidateRows, int limit) {
        boolean hasNext = candidateRows.size() > limit;
        if (hasNext) {
            candidateRows = candidateRows.subList(0, limit);
        }

        List<Long> postIds = candidateRows.stream()
                .map(row -> row.get(post.id))
                .toList();
        String nextCursor = hasNext && !candidateRows.isEmpty()
                ? encodeLatestCursor(candidateRows.getLast().get(post.createdAt), candidateRows.getLast().get(post.id))
                : null;

        return new FeedPageQueryResult(readFeedPosts(memberId, postIds), nextCursor, hasNext);
    }

    private FeedPageQueryResult toTrendingFeedPageQueryResult(Long memberId, List<Tuple> candidateRows, int limit) {
        boolean hasNext = candidateRows.size() > limit;
        if (hasNext) {
            candidateRows = candidateRows.subList(0, limit);
        }

        List<Long> postIds = candidateRows.stream()
                .map(row -> row.get(post.id))
                .toList();
        String nextCursor = hasNext && !candidateRows.isEmpty()
                ? encodeTrendingCursor(
                        numberValue(candidateRows.getLast().get(TRENDING_SCORE)),
                        candidateRows.getLast().get(post.createdAt),
                        candidateRows.getLast().get(post.id)
                )
                : null;

        return new FeedPageQueryResult(readFeedPosts(memberId, postIds), nextCursor, hasNext);
    }

    private List<FeedPostQueryDto> readFeedPosts(Long memberId, List<Long> postIds) {
        if (postIds.isEmpty()) {
            return List.of();
        }

        Expression<Boolean> isMineExpression = isMineExpression(memberId);
        Expression<Boolean> likedExpression = likedExpression(memberId);
        Expression<Boolean> bookmarkedExpression = bookmarkedExpression(memberId);
        Expression<String> contentPreviewExpression = contentPreviewExpression();

        List<Tuple> rows = queryFactory
                .select(
                        post.id,
                        board.id,
                        board.boardType,
                        board.name,
                        board.parentId,
                        post.category,
                        post.title,
                        contentPreviewExpression,
                        POST_TAG,
                        THUMBNAIL_IMAGE.imageUrl,
                        post.isAnonymous,
                        post.isPinned,
                        post.isExternalVisible,
                        isMineExpression,
                        likedExpression,
                        bookmarkedExpression,
                        Expressions.asBoolean(false),
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
                .join(board).on(board.id.eq(post.boardId))
                .join(member).on(member.id.eq(post.memberId))
                .leftJoin(memberProfile).on(memberProfile.memberId.eq(member.id))
                .leftJoin(post.tags, POST_TAG)
                .leftJoin(THUMBNAIL_IMAGE).on(
                        THUMBNAIL_IMAGE.post.eq(post),
                        THUMBNAIL_IMAGE.displayOrder.eq(0)
                )
                .leftJoin(postLikeCount).on(postLikeCount.postId.eq(post.id))
                .leftJoin(postCommentCount).on(postCommentCount.postId.eq(post.id))
                .leftJoin(postBookmarkCount).on(postBookmarkCount.postId.eq(post.id))
                .leftJoin(postViewCount).on(postViewCount.postId.eq(post.id))
                .leftJoin(MEMBER_POST_LIKE).on(
                        MEMBER_POST_LIKE.postId.eq(post.id),
                        MEMBER_POST_LIKE.memberId.eq(resolveMemberId(memberId))
                )
                .leftJoin(MEMBER_POST_BOOKMARK).on(
                        MEMBER_POST_BOOKMARK.postId.eq(post.id),
                        MEMBER_POST_BOOKMARK.memberId.eq(resolveMemberId(memberId))
                )
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
                .map(postId -> toFeedPostQueryDto(
                        rowByPostId.get(postId),
                        new ArrayList<>(tagsByPostId.getOrDefault(postId, new LinkedHashSet<>()))
                ))
                .toList();
    }

    private FeedPostQueryDto toFeedPostQueryDto(Tuple row, List<String> tags) {
        return new FeedPostQueryDto(
                row.get(post.id),
                row.get(board.id),
                row.get(board.boardType),
                row.get(board.name),
                row.get(board.parentId),
                row.get(post.category),
                row.get(post.title),
                row.get(CONTENT_PREVIEW),
                tags,
                row.get(THUMBNAIL_IMAGE.imageUrl),
                booleanValue(row.get(post.isAnonymous)),
                booleanValue(row.get(post.isPinned)),
                booleanValue(row.get(post.isExternalVisible)),
                booleanValue(row.get(IS_MINE)),
                booleanValue(row.get(LIKED)),
                booleanValue(row.get(BOOKMARKED)),
                false,
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

    private List<Long> readMajorBoardIds(Long memberId) {
        return queryFactory.select(major.boardId)
                .from(memberMajor)
                .join(major).on(major.id.eq(memberMajor.majorId))
                .where(memberMajor.member.id.eq(memberId))
                .fetch();
    }

    private List<Long> readInterestBoardIds(Long memberId) {
        NumberPath<Long> interestId = Expressions.numberPath(Long.class, "interestId");
        return queryFactory.select(interest.boardId)
                .from(member)
                .join(member.interests, interestId)
                .join(interest).on(interest.id.eq(interestId))
                .where(member.id.eq(memberId))
                .fetch();
    }

    private BooleanExpression basePostCondition(Collection<Long> blockedMemberIds,
                                                Collection<Long> readableGroupBoardIds) {
        return post.status.eq(PostStatus.ACTIVE)
                .and(board.isActive.isTrue())
                .and(member.status.eq(MemberStatus.ACTIVE))
                .and(readableBoardCondition(readableGroupBoardIds))
                .and(authorNotBlocked(blockedMemberIds));
    }

    private BooleanExpression readableBoardCondition(Collection<Long> readableGroupBoardIds) {
        if (readableGroupBoardIds == null || readableGroupBoardIds.isEmpty()) {
            return board.boardType.ne(BoardType.GROUP);
        }
        return board.boardType.ne(BoardType.GROUP)
                .or(board.id.in(readableGroupBoardIds));
    }

    private BooleanExpression authorNotBlocked(Collection<Long> blockedMemberIds) {
        if (blockedMemberIds == null || blockedMemberIds.isEmpty()) {
            return null;
        }
        return post.memberId.notIn(blockedMemberIds);
    }

    private BooleanExpression categoryEq(PostCategory category) {
        return category == null ? null : post.category.eq(category);
    }

    private BooleanExpression latestCursorCondition(LocalDateTime cursorCreatedAt, Long cursorPostId) {
        if (cursorCreatedAt == null || cursorPostId == null) {
            return null;
        }
        return post.createdAt.lt(cursorCreatedAt)
                .or(post.createdAt.eq(cursorCreatedAt).and(post.id.lt(cursorPostId)));
    }

    private BooleanExpression trendingCursorCondition(NumberExpression<Long> scoreExpression,
                                                      Long cursorScore,
                                                      LocalDateTime cursorCreatedAt,
                                                      Long cursorPostId) {
        if (cursorScore == null || cursorCreatedAt == null || cursorPostId == null) {
            return null;
        }
        return scoreExpression.lt(cursorScore)
                .or(scoreExpression.eq(cursorScore).and(post.createdAt.lt(cursorCreatedAt)))
                .or(scoreExpression.eq(cursorScore)
                        .and(post.createdAt.eq(cursorCreatedAt))
                        .and(post.id.lt(cursorPostId)));
    }

    private BooleanExpression resolveHomeSourceCondition(HomeFeedFilter filter,
                                                         Long universityId,
                                                         Collection<Long> subscribedBoardIds) {
        BooleanExpression subscribedBoardCondition = subscribedBoardCondition(subscribedBoardIds);
        BooleanExpression sameUniversityCondition = universityId == null ? null : member.universityId.eq(universityId);

        return switch (filter) {
            case SUBSCRIBED -> subscribedBoardCondition == null ? alwaysFalse() : subscribedBoardCondition;
            case RECOMMENDED -> {
                BooleanExpression recommendedCondition = resolveRecommendedCondition(universityId, subscribedBoardIds);
                yield recommendedCondition == null ? alwaysFalse() : recommendedCondition;
            }
            case ALL -> anyOf(subscribedBoardCondition, sameUniversityCondition);
        };
    }

    private BooleanExpression resolveRecommendedCondition(Long universityId, Collection<Long> subscribedBoardIds) {
        BooleanExpression sameUniversityCondition = universityId == null ? null : member.universityId.eq(universityId);
        if (sameUniversityCondition == null) {
            return null;
        }
        if (subscribedBoardIds == null || subscribedBoardIds.isEmpty()) {
            return sameUniversityCondition;
        }
        return sameUniversityCondition.and(post.boardId.notIn(subscribedBoardIds));
    }

    private BooleanExpression resolveFollowingSourceCondition(FollowingFeedScope scope,
                                                              Collection<Long> followingMemberIds,
                                                              Collection<Long> myGroupBoardIds) {
        BooleanExpression followingCondition = followingMemberCondition(followingMemberIds);
        BooleanExpression groupCondition = myGroupBoardCondition(myGroupBoardIds);

        return switch (scope) {
            case FOLLOWING_MEMBERS -> followingCondition == null ? alwaysFalse() : followingCondition;
            case MY_GROUPS -> groupCondition == null ? alwaysFalse() : groupCondition;
            case ALL -> {
                BooleanExpression combinedCondition = anyOf(followingCondition, groupCondition);
                yield combinedCondition == null ? alwaysFalse() : combinedCondition;
            }
        };
    }

    private BooleanExpression subscribedBoardCondition(Collection<Long> subscribedBoardIds) {
        if (subscribedBoardIds == null || subscribedBoardIds.isEmpty()) {
            return null;
        }
        return post.boardId.in(subscribedBoardIds);
    }

    private BooleanExpression followingMemberCondition(Collection<Long> followingMemberIds) {
        if (followingMemberIds == null || followingMemberIds.isEmpty()) {
            return null;
        }
        return post.memberId.in(followingMemberIds);
    }

    private BooleanExpression myGroupBoardCondition(Collection<Long> myGroupBoardIds) {
        if (myGroupBoardIds == null || myGroupBoardIds.isEmpty()) {
            return null;
        }
        return post.boardId.in(myGroupBoardIds);
    }

    private BooleanExpression anyOf(BooleanExpression left, BooleanExpression right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        return left.or(right);
    }

    private BooleanExpression alwaysFalse() {
        return Expressions.asBoolean(false).isTrue();
    }

    private Expression<Boolean> isMineExpression(Long memberId) {
        if (memberId == null) {
            return ExpressionUtils.as(Expressions.asBoolean(false), IS_MINE);
        }
        return ExpressionUtils.as(post.memberId.eq(memberId), IS_MINE);
    }

    private Expression<Boolean> likedExpression(Long memberId) {
        if (memberId == null) {
            return ExpressionUtils.as(Expressions.asBoolean(false), LIKED);
        }
        return ExpressionUtils.as(MEMBER_POST_LIKE.id.isNotNull(), LIKED);
    }

    private Expression<Boolean> bookmarkedExpression(Long memberId) {
        if (memberId == null) {
            return ExpressionUtils.as(Expressions.asBoolean(false), BOOKMARKED);
        }
        return ExpressionUtils.as(MEMBER_POST_BOOKMARK.id.isNotNull(), BOOKMARKED);
    }

    private Expression<String> contentPreviewExpression() {
        StringExpression contentPreview = Expressions.stringTemplate(
                "substring({0}, 1, {1})",
                post.content,
                Expressions.constant(CONTENT_PREVIEW_LENGTH)
        );
        return ExpressionUtils.as(contentPreview, CONTENT_PREVIEW);
    }

    private NumberExpression<Long> trendingScoreExpression() {
        return postLikeCount.likeCount.coalesce(0).longValue().multiply(5L)
                .add(postCommentCount.commentCount.coalesce(0).longValue().multiply(4L))
                .add(postBookmarkCount.bookmarkCount.coalesce(0).longValue().multiply(3L))
                .add(postViewCount.viewCount.coalesce(0).longValue());
    }

    private String encodeLatestCursor(LocalDateTime createdAt, Long postId) {
        return createdAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                + CURSOR_DELIMITER
                + postId;
    }

    private String encodeTrendingCursor(Long score, LocalDateTime createdAt, Long postId) {
        return score
                + CURSOR_DELIMITER
                + createdAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                + CURSOR_DELIMITER
                + postId;
    }

    private Long resolveMemberId(Long memberId) {
        return memberId == null ? UNKNOWN_MEMBER_ID : memberId;
    }

    private boolean booleanValue(Boolean value) {
        return Boolean.TRUE.equals(value);
    }

    private long numberValue(Number value) {
        return value == null ? 0L : value.longValue();
    }
}
