package cluverse.post.controller;

import cluverse.common.api.response.ApiResponse;
import cluverse.post.properties.PostImageUploadProperties;
import cluverse.post.service.implement.PostImageUploadExperimentAuthorizer;
import cluverse.post.service.response.PostImageUploadBenchmarkReadinessResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/image-uploads/benchmark-readiness")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "image-upload-experiment", name = "enabled", havingValue = "true")
public class PostImageUploadBenchmarkController {

    private final PostImageUploadExperimentAuthorizer authorizer;
    private final PostImageUploadProperties properties;

    @GetMapping
    public ApiResponse<PostImageUploadBenchmarkReadinessResponse> readiness(
            @RequestHeader(value = "X-Benchmark-Token", required = false) String benchmarkToken
    ) {
        authorizer.authorize(benchmarkToken);
        return ApiResponse.ok(PostImageUploadBenchmarkReadinessResponse.from(properties));
    }
}
