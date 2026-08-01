package cluverse.popularity.service.response;

import cluverse.popularity.domain.PopularityAlgorithmVersion;

public record PopularityRunResponse(PopularityAlgorithmVersion version, int examinedPostCount) {
}
