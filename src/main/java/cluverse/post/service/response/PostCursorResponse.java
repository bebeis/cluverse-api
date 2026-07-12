package cluverse.post.service.response;

import java.time.LocalDateTime;

/**
 * 커서 = 페이지 경계 게시글의 (createdAt, postId).
 * 다음/이전 페이지 요청 시 cursorCreatedAt/cursorPostId로 그대로 넘긴다.
 */
public record PostCursorResponse(
        LocalDateTime createdAt,
        Long postId
) {
}
