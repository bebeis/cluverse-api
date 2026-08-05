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
import java.util.concurrent.Semaphore;

@Component
public class PostImageUploadProcessorV3 extends AbstractPostImageUploadProcessor {

    private final ExecutorService executor;
    private final Semaphore remoteCallSemaphore;

    public PostImageUploadProcessorV3(
            PostImageObjectStorageClient storageClient,
            PostImageProcessorClient processorClient,
            PostImageUploadMetricsRecorder metricsRecorder,
            @Qualifier("postImageVirtualExecutor") ExecutorService executor,
            Semaphore postImageRemoteCallSemaphore
    ) {
        super(storageClient, processorClient, metricsRecorder);
        this.executor = executor;
        this.remoteCallSemaphore = postImageRemoteCallSemaphore;
    }

    @Override
    public ImageUploadVersion version() {
        return ImageUploadVersion.V3;
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
                futures.add(CompletableFuture.supplyAsync(
                        () -> processWithPermit(image, failurePoint), executor));
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

    private ProcessedPostImage processWithPermit(
            PreparedPostImage image,
            ImageUploadFailurePoint failurePoint
    ) {
        long waitingAt = System.nanoTime();
        try {
            remoteCallSemaphore.acquire();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("이미지 처리 대기 중 요청이 중단됐습니다.", exception);
        }
        recordWait("semaphore", System.nanoTime() - waitingAt);
        try {
            return processOne(image, failurePoint);
        } finally {
            remoteCallSemaphore.release();
        }
    }
}
