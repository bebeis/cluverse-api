package cluverse.post.service.response;

import cluverse.post.domain.PostCategory;

import java.time.LocalDateTime;
import java.util.List;

public record PostSummaryResponse(
        Long postId,
        Long boardId,
        PostCategory category,
        String title,
        String contentPreview,
        List<String> tags,
        String thumbnailImageUrl,
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
        LocalDateTime createdAt
) {
    public PostSummaryResponse {
        tags = tags == null ? List.of() : List.copyOf(tags);
    }
}
