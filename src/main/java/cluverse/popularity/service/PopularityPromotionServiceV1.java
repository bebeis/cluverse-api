package cluverse.popularity.service;

import cluverse.popularity.domain.PopularityAlgorithmVersion;
import cluverse.popularity.service.implement.PopularityBatchProcessorV1;
import cluverse.popularity.service.implement.PopularityExperimentAuthorizer;
import cluverse.popularity.service.response.PopularityRunResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PopularityPromotionServiceV1 {

    private final PopularityBatchProcessorV1 popularityBatchProcessorV1;
    private final PopularityExperimentAuthorizer popularityExperimentAuthorizer;

    public PopularityRunResponse run(String benchmarkToken) {
        popularityExperimentAuthorizer.authorize(benchmarkToken);
        return new PopularityRunResponse(PopularityAlgorithmVersion.V1, popularityBatchProcessorV1.run());
    }
}
