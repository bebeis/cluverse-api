package cluverse.post.service.implement;

import cluverse.post.domain.Post;
import cluverse.post.domain.PostCategory;

import java.time.LocalDateTime;

public record PostCreatedEvent(
        Long boardId,
        Long postId,
        PostCategory category,
        LocalDateTime createdAt
) {
    public static PostCreatedEvent from(Post post) {
        return new PostCreatedEvent(
                post.getBoardId(),
                post.getId(),
                post.getCategory(),
                post.getCreatedAt()
        );
    }
}
