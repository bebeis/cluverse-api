package cluverse.feed.service.response;

import java.util.List;

public record FeedPageResponse(
        List<FeedPostSummaryResponse> posts,
        String nextCursor,
        int limit,
        boolean hasNext
) {
    public FeedPageResponse {
        posts = posts == null ? List.of() : List.copyOf(posts);
    }

    public static FeedPageResponse empty(int limit) {
        return new FeedPageResponse(List.of(), null, limit, false);
    }
}
