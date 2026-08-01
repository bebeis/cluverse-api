package cluverse.place.controller;

import cluverse.common.api.response.ApiResponse;
import cluverse.place.service.LocalMapBenchmarkService;
import cluverse.place.service.implement.LocalMapExperimentAuthorizer;
import cluverse.place.service.response.LocalMapBenchmarkReadinessResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/local-map")
@RequiredArgsConstructor
public class LocalMapBenchmarkController {

    private final LocalMapBenchmarkService benchmarkService;
    private final LocalMapExperimentAuthorizer experimentAuthorizer;

    @GetMapping("/benchmark-readiness")
    public ApiResponse<LocalMapBenchmarkReadinessResponse> readiness(
            @RequestHeader(value = "X-Benchmark-Token", required = false) String benchmarkToken
    ) {
        experimentAuthorizer.authorize(benchmarkToken);
        return ApiResponse.ok(benchmarkService.readReadiness());
    }
}
