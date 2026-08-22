package cluverse.post.service.response;

import cluverse.post.properties.PostImageProcessorMode;
import cluverse.post.properties.PostImageUploadProperties;

public record PostImageUploadBenchmarkReadinessResponse(
        boolean experimentEnabled,
        PostImageProcessorMode processorMode,
        long stubAverageDelayMillis,
        int maxConcurrentRemoteCalls,
        int virtualMaxConcurrentTasks,
        int platformQueueCapacity
) {
    public static PostImageUploadBenchmarkReadinessResponse from(PostImageUploadProperties properties) {
        return new PostImageUploadBenchmarkReadinessResponse(
                properties.enabled(),
                properties.processorMode(),
                properties.stubAverageDelay().toMillis(),
                properties.maxConcurrentRemoteCalls(),
                properties.virtualMaxConcurrentTasks(),
                properties.platformQueueCapacity()
        );
    }
}
