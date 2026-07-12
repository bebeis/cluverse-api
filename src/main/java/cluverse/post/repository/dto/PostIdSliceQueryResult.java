package cluverse.post.repository.dto;

import java.util.List;

public record PostIdSliceQueryResult(
        List<Long> postIds,
        boolean hasNext
) {
    public PostIdSliceQueryResult {
        postIds = postIds == null ? List.of() : List.copyOf(postIds);
    }
}
