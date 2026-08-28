package cluverse.post.repository.dto;

import java.util.List;
import java.util.OptionalLong;

public record PostPageQueryResult(
        List<PostSummaryQueryDto> posts,
        boolean hasNext,
        OptionalLong cappedCount
) {
    public PostPageQueryResult {
        posts = posts == null ? List.of() : List.copyOf(posts);
        cappedCount = cappedCount == null ? OptionalLong.empty() : cappedCount;
    }

    public PostPageQueryResult(List<PostSummaryQueryDto> posts, boolean hasNext) {
        this(posts, hasNext, OptionalLong.empty());
    }

    public PostPageQueryResult(List<PostSummaryQueryDto> posts, boolean hasNext, long cappedCount) {
        this(posts, hasNext, OptionalLong.of(cappedCount));
    }
}
