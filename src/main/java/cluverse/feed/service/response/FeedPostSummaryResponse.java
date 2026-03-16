package cluverse.feed.service.response;

import cluverse.feed.repository.dto.FeedPostQueryDto;
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

    public static FeedPostSummaryResponse from(FeedPostQueryDto post) {
        return new FeedPostSummaryResponse(
                post.postId(),
                FeedBoardResponse.from(post),
                post.category(),
                post.title(),
                post.contentPreview(),
                post.tags(),
                post.thumbnailImageUrl(),
                post.isAnonymous(),
                post.isPinned(),
                post.isExternalVisible(),
                post.isMine(),
                post.liked(),
                post.bookmarked(),
                post.hiddenByBlock(),
                post.viewCount(),
                post.likeCount(),
                post.commentCount(),
                post.bookmarkCount(),
                FeedAuthorResponse.visibleOf(
                        post.isAnonymous(),
                        post.isMine(),
                        post.authorMemberId(),
                        post.authorNickname(),
                        post.authorProfileImageUrl()
                ),
                post.createdAt()
        );
    }
}
