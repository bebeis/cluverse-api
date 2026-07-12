package cluverse.post.service.response;

import java.util.List;

/**
 * V4 커서 기반 목록 응답. 페이지 번호/전체 개수 대신
 * 양방향 커서와 존재 여부(hasNext=더 과거, hasPrev=더 최신)를 내려준다.
 */
public record PostCursorPageResponse(
        List<PostSummaryResponse> posts,
        int size,
        boolean hasNext,
        boolean hasPrev,
        PostCursorResponse prevCursor,
        PostCursorResponse nextCursor
) {
    public PostCursorPageResponse {
        posts = posts == null ? List.of() : List.copyOf(posts);
    }
}
