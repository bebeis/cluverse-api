package cluverse.post.service.implement;

import cluverse.post.client.PostImageObjectStorageClient;
import cluverse.post.client.PostImageProcessorClient;
import cluverse.post.domain.ProcessedPostImage;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;

@Component
public class VirtualThreadPostImageUploadProcessor extends AbstractPostImageUploadProcessor {

    private final ExecutorService executor;
    private final Semaphore remoteCallSemaphore;

    public VirtualThreadPostImageUploadProcessor(
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
    public List<ProcessedPostImage> process(List<PreparedPostImage> images) {
        List<CompletableFuture<ProcessedPostImage>> futures = new ArrayList<>();
        RuntimeException submissionFailure = null;
        for (PreparedPostImage image : images) {
            try {
                futures.add(CompletableFuture.supplyAsync(
                        () -> processWithPermit(image), executor));
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

    private ProcessedPostImage processWithPermit(PreparedPostImage image) {
        long waitingAt = System.nanoTime();
        try {
            remoteCallSemaphore.acquire();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("이미지 처리 대기 중 요청이 중단됐습니다.", exception);
        }
        recordWait("semaphore", System.nanoTime() - waitingAt);
        try {
            return processOne(image);
        } finally {
            remoteCallSemaphore.release();
        }
    }
}
