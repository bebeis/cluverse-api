package cluverse.post.service.response;

import cluverse.post.domain.PostCategory;

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
        boolean isMine,
        boolean likedByMe,
        boolean bookmarkedByMe,
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
}
