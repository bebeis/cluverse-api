package cluverse.popularity.controller;

import cluverse.common.api.response.ApiResponse;
import cluverse.popularity.service.PopularityPromotionServiceV1;
import cluverse.popularity.service.response.PopularityRunResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/popular-posts")
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "popularity",
        name = "experiment-endpoints-enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class PopularityExperimentControllerV1 {

    private final PopularityPromotionServiceV1 popularityPromotionServiceV1;

    @PostMapping("/promotion-runs")
    public ApiResponse<PopularityRunResponse> runPromotion(
            @RequestHeader(name = "X-Benchmark-Token", required = false) String benchmarkToken
    ) {
        return ApiResponse.ok(popularityPromotionServiceV1.run(benchmarkToken));
    }
}
