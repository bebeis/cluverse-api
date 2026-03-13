package cluverse.post.service.response;

import cluverse.post.domain.PostCategory;
import cluverse.post.repository.dto.PostDetailQueryDto;

import java.time.LocalDateTime;
import java.util.List;

public record PostDetailResponse(
        Long postId,
        Long boardId,
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
    public PostDetailResponse {
        tags = tags == null ? List.of() : List.copyOf(tags);
        imageUrls = imageUrls == null ? List.of() : List.copyOf(imageUrls);
    }

    public static PostDetailResponse from(PostDetailQueryDto post) {
        return new PostDetailResponse(
                post.postId(),
                post.boardId(),
                post.category(),
                post.title(),
                post.content(),
                post.tags(),
                post.imageUrls(),
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
}
