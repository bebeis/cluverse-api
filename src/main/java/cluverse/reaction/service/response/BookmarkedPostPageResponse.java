package cluverse.reaction.service.response;

import cluverse.feed.service.response.FeedPostSummaryResponse;
import cluverse.reaction.service.request.BookmarkedPostSortType;

import java.util.List;

public record BookmarkedPostPageResponse(
        List<FeedPostSummaryResponse> posts,
        int page,
        int size,
        boolean hasNext,
        BookmarkedPostSortType sort
) {
    public BookmarkedPostPageResponse {
        posts = posts == null ? List.of() : List.copyOf(posts);
    }
}
