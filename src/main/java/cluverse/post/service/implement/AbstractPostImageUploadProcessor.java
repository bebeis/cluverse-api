package cluverse.post.service.implement;

import cluverse.common.exception.ExternalServiceException;
import cluverse.post.client.PostImageObjectStorageClient;
import cluverse.post.client.PostImageProcessorClient;
import cluverse.post.domain.PostImageMetadata;
import cluverse.post.domain.ProcessedPostImage;
import cluverse.post.domain.ImageUploadVersion;

abstract class AbstractPostImageUploadProcessor implements PostImageUploadProcessor {

    private final PostImageObjectStorageClient storageClient;
    private final PostImageProcessorClient processorClient;
    private final PostImageUploadMetricsRecorder metricsRecorder;

    protected AbstractPostImageUploadProcessor(
            PostImageObjectStorageClient storageClient,
            PostImageProcessorClient processorClient,
            PostImageUploadMetricsRecorder metricsRecorder
    ) {
        this.storageClient = storageClient;
        this.processorClient = processorClient;
        this.metricsRecorder = metricsRecorder;
    }

    protected final ProcessedPostImage processOne(PreparedPostImage image) {
        storageClient.upload(
                image.command().stagingKey(),
                image.contentType(),
                image.path()
        );
        long startedAt = System.nanoTime();
        ProcessedPostImage result = processorClient.process(image.command());
        metricsRecorder.remote(ImageUploadVersion.V3, System.nanoTime() - startedAt);
        return verifyStoredResult(result);
    }

    protected final void recordWait(String kind, long elapsedNanos) {
        metricsRecorder.waitTime(ImageUploadVersion.V3, kind, elapsedNanos);
    }

    protected final RuntimeException unwrapCompletion(RuntimeException exception) {
        Throwable cause = exception.getCause();
        return cause instanceof RuntimeException runtimeException ? runtimeException : exception;
    }

    private ProcessedPostImage verifyStoredResult(ProcessedPostImage result) {
        PostImageMetadata content = verifyStoredObject(result.content());
        PostImageMetadata thumbnail = verifyStoredObject(result.thumbnail());
        return new ProcessedPostImage(result.displayOrder(), content, thumbnail);
    }

    private PostImageMetadata verifyStoredObject(PostImageMetadata metadata) {
        if (metadata == null) {
            throw new ExternalServiceException(
                    "이미지 프로세서 결과 객체를 확인할 수 없습니다.",
                    new IllegalStateException("missing object")
            );
        }
        long actualBytes = storageClient.size(metadata.objectKey());
        return new PostImageMetadata(
                metadata.objectKey(), metadata.contentType(), metadata.width(), metadata.height(), actualBytes);
    }

}
