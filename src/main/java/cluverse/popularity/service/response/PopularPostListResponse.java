package cluverse.popularity.service.response;

import cluverse.popularity.domain.PopularPostSortType;

import java.util.List;

public record PopularPostListResponse(
        PopularPostSortType sort,
        List<PopularPostResponse> posts
) {
}
