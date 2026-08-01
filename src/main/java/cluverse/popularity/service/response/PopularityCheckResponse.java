package cluverse.popularity.service.response;

import cluverse.popularity.domain.PopularityAlgorithmVersion;

public record PopularityCheckResponse(PopularityAlgorithmVersion version, Long postId) {
}
