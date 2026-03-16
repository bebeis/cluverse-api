package cluverse.feed.service.response;

import cluverse.post.domain.PostCategory;

import java.time.LocalDateTime;
import java.util.List;

public record FeedPostSummaryResponse(
        Long postId,
        FeedBoardResponse board,
        PostCategory category,
        String title,
        String contentPreview,
        List<String> tags,
        String thumbnailImageUrl,
        boolean isAnonymous,
        boolean isPinned,
        boolean isExternalVisible,
        boolean isMine,
        boolean liked,
        boolean bookmarked,
        boolean hiddenByBlock,
        long viewCount,
        long likeCount,
        long commentCount,
        long bookmarkCount,
        FeedAuthorResponse author,
        LocalDateTime createdAt
) {
    public FeedPostSummaryResponse {
        tags = tags == null ? List.of() : List.copyOf(tags);
    }
}
