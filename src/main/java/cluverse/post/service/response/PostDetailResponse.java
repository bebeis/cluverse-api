package cluverse.post.service.response;

import cluverse.post.domain.PostCategory;
import cluverse.post.repository.dto.PostDetailQueryDto;

import java.time.LocalDateTime;
import java.util.List;

public record PostDetailResponse(
        Long postId,
        Long boardId,
        PostBoardResponse board,
        PostCategory category,
        String title,
        String content,
        List<String> tags,
        List<String> imageUrls,
        List<PostImageResponse> imageAssets,
        boolean isAnonymous,
        boolean isPinned,
        boolean isExternalVisible,
        long viewCount,
        long likeCount,
        long commentCount,
        long bookmarkCount,
        PostAuthorResponse author,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public PostDetailResponse {
        tags = tags == null ? List.of() : List.copyOf(tags);
        imageUrls = imageUrls == null ? List.of() : List.copyOf(imageUrls);
        imageAssets = imageAssets == null ? List.of() : List.copyOf(imageAssets);
    }

    public PostDetailResponse(
            Long postId,
            Long boardId,
            PostBoardResponse board,
            PostCategory category,
            String title,
            String content,
            List<String> tags,
            List<String> imageUrls,
            boolean isAnonymous,
            boolean isPinned,
            boolean isExternalVisible,
            long viewCount,
            long likeCount,
            long commentCount,
            long bookmarkCount,
            PostAuthorResponse author,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this(postId, boardId, board, category, title, content, tags, imageUrls,
                List.of(), isAnonymous, isPinned, isExternalVisible, viewCount,
                likeCount, commentCount, bookmarkCount, author, createdAt, updatedAt);
    }

    public static PostDetailResponse from(PostDetailQueryDto post) {
        return new PostDetailResponse(
                post.postId(),
                post.boardId(),
                PostBoardResponse.from(post),
                post.category(),
                post.title(),
                post.content(),
                post.tags(),
                post.imageUrls(),
                post.imageAssets().stream().map(PostImageResponse::from).toList(),
                post.isAnonymous(),
                post.isPinned(),
                post.isExternalVisible(),
                post.viewCount(),
                post.likeCount(),
                post.commentCount(),
                post.bookmarkCount(),
                PostAuthorResponse.visibleOf(
                        post.isAnonymous(),
                        post.isMine(),
                        post.authorMemberId(),
                        post.authorNickname(),
                        post.authorProfileImageUrl()
                ),
                post.createdAt(),
                post.updatedAt()
        );
    }

    public PostDetailResponse withViewCount(long currentViewCount) {
        return new PostDetailResponse(
                postId,
                boardId,
                board,
                category,
                title,
                content,
                tags,
                imageUrls,
                imageAssets,
                isAnonymous,
                isPinned,
                isExternalVisible,
                currentViewCount,
                likeCount,
                commentCount,
                bookmarkCount,
                author,
                createdAt,
                updatedAt
        );
    }
}
