package cluverse.popularity.controller;

import cluverse.common.api.response.ApiResponse;
import cluverse.popularity.domain.PopularPostSortType;
import cluverse.popularity.domain.PopularityAlgorithmVersion;
import cluverse.popularity.service.PopularPostQueryService;
import cluverse.popularity.service.response.PopularPostListResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/popular-posts")
@RequiredArgsConstructor
public class PopularPostControllerV1 {

    private final PopularPostQueryService popularPostQueryService;

    @GetMapping("/recent")
    public ApiResponse<PopularPostListResponse> getRecent(
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ApiResponse.ok(popularPostQueryService.getRecent(PopularityAlgorithmVersion.V1, size));
    }

    @GetMapping("/history")
    public ApiResponse<PopularPostListResponse> getHistory(
            @RequestParam(defaultValue = "LATEST") PopularPostSortType sort,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ApiResponse.ok(popularPostQueryService.getHistory(PopularityAlgorithmVersion.V1, sort, size));
    }
}
