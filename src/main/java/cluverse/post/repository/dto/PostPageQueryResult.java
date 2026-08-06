package cluverse.post.repository.dto;

import java.util.List;

public record PostPageQueryResult(
        List<PostSummaryQueryDto> posts,
        boolean hasNext,
        Long cappedCount
) {
    public PostPageQueryResult {
        posts = posts == null ? List.of() : List.copyOf(posts);
    }

    public PostPageQueryResult(List<PostSummaryQueryDto> posts, boolean hasNext) {
        this(posts, hasNext, null);
    }
}
