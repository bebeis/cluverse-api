package cluverse.post.repository.dto;

import cluverse.board.domain.BoardType;
import cluverse.post.domain.PostCategory;

import java.time.LocalDateTime;
import java.util.List;

public record PostDetailQueryDto(
        Long postId,
        Long boardId,
        BoardType boardType,
        String boardName,
        Long parentBoardId,
        PostCategory category,
        String title,
        String content,
        List<String> tags,
        List<String> imageUrls,
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
    public PostDetailQueryDto {
        tags = tags == null ? List.of() : List.copyOf(tags);
        imageUrls = imageUrls == null ? List.of() : List.copyOf(imageUrls);
    }
}
