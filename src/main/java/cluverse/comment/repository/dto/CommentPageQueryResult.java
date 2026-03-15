package cluverse.comment.repository.dto;

import java.util.List;

public record CommentPageQueryResult(
        List<CommentQueryDto> comments,
        boolean hasNext
) {
    public CommentPageQueryResult {
        comments = comments == null ? List.of() : List.copyOf(comments);
    }
}
