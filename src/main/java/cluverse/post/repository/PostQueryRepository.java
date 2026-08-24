package cluverse.post.repository;

import cluverse.board.domain.BoardType;
import cluverse.post.domain.PostCategory;
import cluverse.post.domain.PostStatus;
import cluverse.post.domain.QPostImage;
import cluverse.post.client.PostImageObjectStorageClient;
import cluverse.post.repository.dto.PostDetailQueryDto;
import cluverse.post.repository.dto.PostImageQueryDto;
import cluverse.post.repository.dto.PostSummaryQueryDto;
import com.querydsl.core.types.Expression;
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
    private final PostImageObjectStorageClient imageStorageClient;

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
                        postViewCount.viewCount.coalesce(0L).longValue(),
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
        List<PostImageRow> images = queryFactory.select(Projections.constructor(
                        PostImageRow.class,
                        postImage.imageUrl,
                        postImage.contentKey,
                        postImage.thumbnailKey
                ))
                .from(postImage)
                .where(postImage.post.id.eq(postId))
                .orderBy(postImage.displayOrder.asc())
                .fetch();

        List<String> imageUrls = images.stream()
                .map(image -> resolveImageUrl(image.legacyImageUrl(), image.contentKey()))
                .toList();
        List<PostImageQueryDto> imageAssets = images.stream()
                .filter(image -> image.contentKey() != null)
                .map(image -> new PostImageQueryDto(
                        image.contentKey(),
                        image.thumbnailKey(),
                        resolveImageUrl(null, image.contentKey()),
                        resolveImageUrl(null, image.thumbnailKey())
                ))
                .toList();

        return Optional.of(row.toDto(tags, imageUrls, imageAssets));
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
                        THUMBNAIL_IMAGE.contentKey,
                        THUMBNAIL_IMAGE.thumbnailKey,
                        post.isAnonymous,
                        post.isPinned,
                        post.isExternalVisible,
                        isMineExpression(memberId),
                        postViewCount.viewCount.coalesce(0L).longValue(),
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
                .filter(rowByPostId::containsKey)
                .map(postId -> {
                    PostSummaryRow row = rowByPostId.get(postId);
                    String thumbnailKey = row.thumbnailImageKey() != null
                            ? row.thumbnailImageKey() : row.contentImageKey();
                    return row.toDto(
                            tagsByPostId.getOrDefault(postId, List.of()),
                            resolveImageUrl(row.legacyThumbnailImageUrl(), thumbnailKey)
                    );
                })
                .toList();
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

    private String resolveImageUrl(String legacyImageUrl, String objectKey) {
        if (objectKey != null) {
            return imageStorageClient.createImageUrl(objectKey);
        }
        return legacyImageUrl;
    }

    public record PostImageRow(
            String legacyImageUrl,
            String contentKey,
            String thumbnailKey
    ) {
    }

    // Projections.constructor는 public 생성자만 탐색하므로 반드시 public record여야 한다.
    public record PostSummaryRow(
            Long postId,
            Long boardId,
            PostCategory category,
            String title,
            String contentPreview,
            String legacyThumbnailImageUrl,
            String contentImageKey,
            String thumbnailImageKey,
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
        private PostSummaryQueryDto toDto(List<String> tags, String thumbnailImageUrl) {
            return new PostSummaryQueryDto(
                    postId, boardId, category, title, contentPreview, tags,
                    thumbnailImageUrl,
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
        private PostDetailQueryDto toDto(
                List<String> tags,
                List<String> imageUrls,
                List<PostImageQueryDto> imageAssets
        ) {
            return new PostDetailQueryDto(
                    postId, boardId, boardType, boardName, parentBoardId, category, title, content,
                    tags, imageUrls, imageAssets,
                    isAnonymous, isPinned, isExternalVisible, isMine,
                    viewCount, likeCount, commentCount, bookmarkCount,
                    authorMemberId, authorNickname, authorProfileImageUrl, createdAt, updatedAt
            );
        }
    }
}
