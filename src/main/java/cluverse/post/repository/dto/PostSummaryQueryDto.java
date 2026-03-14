package cluverse.post.repository.dto;

import cluverse.post.domain.PostCategory;

import java.time.LocalDateTime;
import java.util.List;

public record PostSummaryQueryDto(
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
        long viewCount,
        long likeCount,
        long commentCount,
        long bookmarkCount,
        Long authorMemberId,
        String authorNickname,
        String authorProfileImageUrl,
        LocalDateTime createdAt
) {
    public PostSummaryQueryDto {
        tags = tags == null ? List.of() : List.copyOf(tags);
    }
}
