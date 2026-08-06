package cluverse.post.repository.dto;

import java.util.List;

public record CachedLatestPostIds(
        List<Long> postIds,
        long cachedCount
) {
    public CachedLatestPostIds {
        postIds = List.copyOf(postIds);
    }
}
