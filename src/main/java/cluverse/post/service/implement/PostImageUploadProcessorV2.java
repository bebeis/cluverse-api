package cluverse.post.service.implement;

import cluverse.post.client.PostImageObjectStorageClient;
import cluverse.post.client.PostImageProcessorClient;
import cluverse.post.domain.ImageUploadVersion;
import cluverse.post.domain.ProcessedPostImage;
import cluverse.post.service.request.ImageUploadFailurePoint;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Component
public class PostImageUploadProcessorV2 extends AbstractPostImageUploadProcessor {

    private final ExecutorService executor;

    public PostImageUploadProcessorV2(
            PostImageObjectStorageClient storageClient,
            PostImageProcessorClient processorClient,
            PostImageUploadMetricsRecorder metricsRecorder,
            @Qualifier("postImagePlatformExecutor") ExecutorService executor
    ) {
        super(storageClient, processorClient, metricsRecorder);
        this.executor = executor;
    }

    @Override
    public ImageUploadVersion version() {
        return ImageUploadVersion.V2;
    }

    @Override
    public List<ProcessedPostImage> process(
            List<PreparedPostImage> images,
            ImageUploadFailurePoint failurePoint
    ) {
        List<CompletableFuture<ProcessedPostImage>> futures = new ArrayList<>();
        RuntimeException submissionFailure = null;
        for (PreparedPostImage image : images) {
            try {
                futures.add(submit(image, failurePoint));
            } catch (RuntimeException exception) {
                submissionFailure = exception;
                break;
            }
        }
        try {
            CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
        } catch (RuntimeException taskFailure) {
            if (submissionFailure == null) {
                throw unwrapCompletion(taskFailure);
            }
        }
        if (submissionFailure != null) {
            throw submissionFailure;
        }
        return futures.stream().map(CompletableFuture::join).toList();
    }

    private CompletableFuture<ProcessedPostImage> submit(
            PreparedPostImage image,
            ImageUploadFailurePoint failurePoint
    ) {
        long submittedAt = System.nanoTime();
        return CompletableFuture.supplyAsync(() -> {
            recordWait("executor_queue", System.nanoTime() - submittedAt);
            return processOne(image, failurePoint);
        }, executor);
    }
}
