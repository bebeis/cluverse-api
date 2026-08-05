package cluverse.certification.controller;

import cluverse.certification.service.CertificationBenchmarkService;
import cluverse.certification.service.implement.CertificationExperimentAuthorizer;
import cluverse.certification.service.response.CertificationBenchmarkReadinessResponse;
import cluverse.common.api.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/certification")
@RequiredArgsConstructor
public class CertificationBenchmarkController {

    private final CertificationBenchmarkService benchmarkService;
    private final CertificationExperimentAuthorizer experimentAuthorizer;

    @GetMapping("/benchmark-readiness")
    public ApiResponse<CertificationBenchmarkReadinessResponse> readiness(
            @RequestHeader(value = "X-Benchmark-Token", required = false) String benchmarkToken
    ) {
        experimentAuthorizer.authorize(benchmarkToken);
        return ApiResponse.ok(benchmarkService.readReadiness());
    }

    @DeleteMapping("/benchmark-cache")
    public ApiResponse<Void> evictCache(
            @RequestHeader(value = "X-Benchmark-Token", required = false) String benchmarkToken
    ) {
        experimentAuthorizer.authorize(benchmarkToken);
        benchmarkService.evictCache();
        return ApiResponse.ok();
    }
}
