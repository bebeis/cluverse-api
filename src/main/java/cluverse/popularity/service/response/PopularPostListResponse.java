package cluverse.popularity.service.response;

import cluverse.popularity.domain.PopularPostSortType;
import cluverse.popularity.domain.PopularityAlgorithmVersion;

import java.util.List;

public record PopularPostListResponse(
        PopularityAlgorithmVersion version,
        PopularPostSortType sort,
        List<PopularPostResponse> posts
) {
}
