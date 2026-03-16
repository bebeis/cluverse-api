package cluverse.feed.repository.dto;

import java.util.List;

public record FeedPageQueryResult(
        List<FeedPostQueryDto> posts,
        String nextCursor,
        boolean hasNext
) {
    public FeedPageQueryResult {
        posts = posts == null ? List.of() : List.copyOf(posts);
    }
}
