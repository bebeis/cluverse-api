package cluverse.post.repository;

import cluverse.board.domain.BoardType;
import cluverse.post.domain.PostCategory;
import cluverse.post.domain.PostStatus;
import cluverse.post.domain.QPostImage;
import cluverse.post.repository.dto.PostDetailQueryDto;
import cluverse.post.repository.dto.PostSummaryQueryDto;
import cluverse.post.service.request.PostOffsetSearchRequest;
import cluverse.post.service.request.PostSortType;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.StringExpression;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static cluverse.board.domain.QBoard.board;
import static cluverse.member.domain.QMember.member;
import static cluverse.member.domain.QMemberProfile.memberProfile;
import static cluverse.meta.domain.QPostBookmarkCount.postBookmarkCount;
import static cluverse.meta.domain.QPostCommentCount.postCommentCount;
import static cluverse.meta.domain.QPostLikeCount.postLikeCount;
import static cluverse.meta.domain.QPostViewCount.postViewCount;
import static cluverse.post.domain.QPost.post;
import static cluverse.post.domain.QPostImage.postImage;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

/**
 * 게시글 화면 데이터(요약/상세) 프로젝션 전담.
 * 페이지에 실을 id 선정은 {@link PostPageQueryRepository}가 담당한다.
 */
@Repository
@RequiredArgsConstructor
public class PostQueryRepository {

    private static final QPostImage THUMBNAIL_IMAGE = new QPostImage("thumbnailImage");
    private static final int CONTENT_PREVIEW_LENGTH = 120;

    private final JPAQueryFactory queryFactory;

    public Optional<PostDetailQueryDto> findPostDetail(Long memberId, Long postId) {
        PostDetailRow row = queryFactory
                .select(Projections.constructor(PostDetailRow.class,
                        post.id,
                        post.boardId,
                        board.boardType,
                        board.name,
                        board.parentId,
                        post.category,
                        post.title,
                        post.content,
                        post.isAnonymous,
                        post.isPinned,
                        post.isExternalVisible,
                        isMineExpression(memberId),
                        postViewCount.viewCount.coalesce(0).longValue(),
                        postLikeCount.likeCount.coalesce(0).longValue(),
                        postCommentCount.commentCount.coalesce(0).longValue(),
                        postBookmarkCount.bookmarkCount.coalesce(0).longValue(),
                        member.id,
                        member.nickname,
                        memberProfile.profileImageUrl,
                        post.createdAt,
                        post.updatedAt
                ))
                .from(post)
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
                .fetchOne();

        if (row == null) {
            return Optional.empty();
        }

        List<String> tags = findTagsByPostIds(List.of(postId)).getOrDefault(postId, List.of());
        List<String> imageUrls = queryFactory.select(postImage.imageUrl)
                .from(postImage)
                .where(postImage.post.id.eq(postId))
                .orderBy(postImage.displayOrder.asc())
                .fetch();

        return Optional.of(row.toDto(tags, imageUrls));
    }

    /**
     * 주어진 id 순서를 그대로 유지해 요약 데이터를 반환한다.
     */
    public List<PostSummaryQueryDto> findPostSummaries(Long memberId, List<Long> postIds) {
        if (postIds.isEmpty()) {
            return List.of();
        }

        List<PostSummaryRow> rows = queryFactory
                .select(Projections.constructor(PostSummaryRow.class,
                        post.id,
                        post.boardId,
                        post.category,
                        post.title,
                        contentPreviewExpression(),
                        THUMBNAIL_IMAGE.imageUrl,
                        post.isAnonymous,
                        post.isPinned,
                        post.isExternalVisible,
                        isMineExpression(memberId),
                        postViewCount.viewCount.coalesce(0).longValue(),
                        postLikeCount.likeCount.coalesce(0).longValue(),
                        postCommentCount.commentCount.coalesce(0).longValue(),
                        postBookmarkCount.bookmarkCount.coalesce(0).longValue(),
                        member.id,
                        member.nickname,
                        memberProfile.profileImageUrl,
                        post.createdAt
                ))
                .from(post)
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

        Map<Long, PostSummaryRow> rowByPostId = rows.stream()
                .collect(toMap(PostSummaryRow::postId, Function.identity()));
        Map<Long, List<String>> tagsByPostId = findTagsByPostIds(postIds);

        return postIds.stream()
                .map(postId -> rowByPostId.get(postId)
                        .toDto(tagsByPostId.getOrDefault(postId, List.of())))
                .toList();
    }

    /**
     * [V1 전용] 인덱스만 건 원본(naive offset) 목록 조회.
     * offset 지점까지의 모든 행에 대해 클러스터드 인덱스 룩업과 7개 조인을 수행한 뒤 버리는,
     * 개선 전 형태를 그대로 보존한 쿼리다. 성능 비교 측정 외 용도로 사용하지 말 것.
     * 태그는 조회하지 않는다(태그 별도 쿼리 조립은 V2에서 도입된 개선이므로 원본에는 없다).
     */
    public List<PostSummaryQueryDto> findPostSummariesWithOffset(Long memberId, PostOffsetSearchRequest request) {
        List<PostSummaryRow> rows = queryFactory
                .select(Projections.constructor(PostSummaryRow.class,
                        post.id,
                        post.boardId,
                        post.category,
                        post.title,
                        contentPreviewExpression(),
                        THUMBNAIL_IMAGE.imageUrl,
                        post.isAnonymous,
                        post.isPinned,
                        post.isExternalVisible,
                        isMineExpression(memberId),
                        postViewCount.viewCount.coalesce(0).longValue(),
                        postLikeCount.likeCount.coalesce(0).longValue(),
                        postCommentCount.commentCount.coalesce(0).longValue(),
                        postBookmarkCount.bookmarkCount.coalesce(0).longValue(),
                        member.id,
                        member.nickname,
                        memberProfile.profileImageUrl,
                        post.createdAt
                ))
                .from(post)
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
                .where(
                        post.status.eq(PostStatus.ACTIVE),
                        post.boardId.eq(request.boardId()),
                        request.category() == null ? null : post.category.eq(request.category())
                )
                .orderBy(resolveOffsetOrderSpecifiers(request.sortOrDefault()))
                .offset(request.offset())
                .limit(request.sizeOrDefault())
                .fetch();

        return rows.stream()
                .map(row -> row.toDto(List.of()))
                .toList();
    }

    private OrderSpecifier<?>[] resolveOffsetOrderSpecifiers(PostSortType sortType) {
        return switch (sortType) {
            case VIEW_COUNT -> new OrderSpecifier<?>[]{postViewCount.viewCount.coalesce(0).desc(), post.id.desc()};
            case LATEST -> new OrderSpecifier<?>[]{post.createdAt.desc(), post.id.desc()};
        };
    }

    private Map<Long, List<String>> findTagsByPostIds(List<Long> postIds) {
        StringPath tag = Expressions.stringPath("tag");
        return queryFactory.select(post.id, tag)
                .from(post)
                .join(post.tags, tag)
                .where(post.id.in(postIds))
                .fetch()
                .stream()
                .collect(groupingBy(row -> row.get(post.id), mapping(row -> row.get(tag), toList())));
    }

    private Expression<Boolean> isMineExpression(Long memberId) {
        if (memberId == null) {
            return Expressions.asBoolean(false);
        }
        return post.memberId.eq(memberId);
    }

    private StringExpression contentPreviewExpression() {
        return Expressions.stringTemplate(
                "substring({0}, 1, {1})",
                post.content,
                Expressions.constant(CONTENT_PREVIEW_LENGTH)
        );
    }

    // Projections.constructor는 public 생성자만 탐색하므로 반드시 public record여야 한다.
    public record PostSummaryRow(
            Long postId,
            Long boardId,
            PostCategory category,
            String title,
            String contentPreview,
            String thumbnailImageUrl,
            boolean isAnonymous,
            boolean isPinned,
            boolean isExternalVisible,
            boolean isMine,
            long viewCount,
            long likeCount,
            long commentCount,
            long bookmarkCount,
            Long authorMemberId,
            String authorNickname,
            String authorProfileImageUrl,
            LocalDateTime createdAt
    ) {
        private PostSummaryQueryDto toDto(List<String> tags) {
            return new PostSummaryQueryDto(
                    postId, boardId, category, title, contentPreview, tags, thumbnailImageUrl,
                    isAnonymous, isPinned, isExternalVisible, isMine,
                    viewCount, likeCount, commentCount, bookmarkCount,
                    authorMemberId, authorNickname, authorProfileImageUrl, createdAt
            );
        }
    }

    public record PostDetailRow(
            Long postId,
            Long boardId,
            BoardType boardType,
            String boardName,
            Long parentBoardId,
            PostCategory category,
            String title,
            String content,
            boolean isAnonymous,
            boolean isPinned,
            boolean isExternalVisible,
            boolean isMine,
            long viewCount,
            long likeCount,
            long commentCount,
            long bookmarkCount,
            Long authorMemberId,
            String authorNickname,
            String authorProfileImageUrl,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        private PostDetailQueryDto toDto(List<String> tags, List<String> imageUrls) {
            return new PostDetailQueryDto(
                    postId, boardId, boardType, boardName, parentBoardId, category, title, content,
                    tags, imageUrls,
                    isAnonymous, isPinned, isExternalVisible, isMine,
                    viewCount, likeCount, commentCount, bookmarkCount,
                    authorMemberId, authorNickname, authorProfileImageUrl, createdAt, updatedAt
            );
        }
    }
}
