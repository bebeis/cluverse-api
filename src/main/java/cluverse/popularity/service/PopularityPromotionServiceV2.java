package cluverse.popularity.service;

import cluverse.popularity.domain.PopularityAlgorithmVersion;
import cluverse.popularity.domain.PopularityTrigger;
import cluverse.popularity.service.implement.PopularityExperimentAuthorizer;
import cluverse.popularity.service.implement.PopularityPromotionProcessorV2;
import cluverse.popularity.service.response.PopularityCheckResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PopularityPromotionServiceV2 {

    private final PopularityPromotionProcessorV2 popularityPromotionProcessorV2;
    private final PopularityExperimentAuthorizer popularityExperimentAuthorizer;

    public PopularityCheckResponse check(Long postId, String benchmarkToken) {
        popularityExperimentAuthorizer.authorize(benchmarkToken);
        popularityPromotionProcessorV2.evaluate(postId, PopularityTrigger.MANUAL);
        return new PopularityCheckResponse(PopularityAlgorithmVersion.V2, postId);
    }
}
