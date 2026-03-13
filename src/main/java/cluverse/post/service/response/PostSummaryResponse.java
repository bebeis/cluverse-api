package cluverse.post.service.response;

import cluverse.post.domain.PostCategory;
import cluverse.post.repository.dto.PostSummaryQueryDto;

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

    public static PostSummaryResponse from(PostSummaryQueryDto post) {
        return new PostSummaryResponse(
                post.postId(),
                post.boardId(),
                post.category(),
                post.title(),
                post.contentPreview(),
                post.tags(),
                post.thumbnailImageUrl(),
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
                post.createdAt()
        );
    }
}
