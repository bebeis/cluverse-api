package cluverse.feed.repository.dto;

import cluverse.board.domain.BoardType;
import cluverse.post.domain.PostCategory;

import java.time.LocalDateTime;
import java.util.List;

public record FeedPostQueryDto(
        Long postId,
        Long boardId,
        BoardType boardType,
        String boardName,
        Long parentBoardId,
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
        Long authorMemberId,
        String authorNickname,
        String authorProfileImageUrl,
        LocalDateTime createdAt
) {
    public FeedPostQueryDto {
        tags = tags == null ? List.of() : List.copyOf(tags);
    }
}
