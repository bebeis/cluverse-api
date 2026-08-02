package cluverse.comment.service.response;

import java.util.List;

public record CommentPageResponse(
        List<CommentResponse> comments,
        String nextCursor,
        int limit,
        boolean hasNext
) {
    public CommentPageResponse {
        comments = comments == null ? List.of() : List.copyOf(comments);
    }
}
