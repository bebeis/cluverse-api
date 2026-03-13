package cluverse.post.repository.dto;

import java.util.List;

public record PostPageQueryResult(
        List<PostSummaryQueryDto> posts,
        boolean hasNext
) {
    public PostPageQueryResult {
        posts = posts == null ? List.of() : List.copyOf(posts);
    }
}
