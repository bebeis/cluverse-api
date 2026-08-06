package cluverse.popularity.controller;

import cluverse.common.api.response.ApiResponse;
import cluverse.popularity.service.PopularityPromotionServiceV2;
import cluverse.popularity.service.response.PopularityCheckResponse;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/popular-posts")
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "popularity",
        name = "experiment-endpoints-enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class PopularityExperimentControllerV2 {

    private final PopularityPromotionServiceV2 popularityPromotionServiceV2;

    @PostMapping("/{postId}/promotion-checks")
    public ApiResponse<PopularityCheckResponse> checkPromotion(
            @PathVariable @Min(1) Long postId,
            @RequestHeader(name = "X-Benchmark-Token", required = false) String benchmarkToken
    ) {
        return ApiResponse.ok(popularityPromotionServiceV2.check(postId, benchmarkToken));
    }
}
