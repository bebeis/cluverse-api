package cluverse.post.service.implement;

import cluverse.post.client.PostImageObjectStorageClient;
import cluverse.post.client.PostImageProcessorClient;
import cluverse.post.domain.ImageUploadVersion;
import cluverse.post.domain.ProcessedPostImage;
import cluverse.post.service.request.ImageUploadFailurePoint;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

@Component
public class PostImageUploadProcessorV1 extends AbstractPostImageUploadProcessor {

    private final Semaphore remoteCallSemaphore;

    public PostImageUploadProcessorV1(
            PostImageObjectStorageClient storageClient,
            PostImageProcessorClient processorClient,
            PostImageUploadMetricsRecorder metricsRecorder,
            Semaphore postImageRemoteCallSemaphore
    ) {
        super(storageClient, processorClient, metricsRecorder);
        this.remoteCallSemaphore = postImageRemoteCallSemaphore;
    }

    @Override
    public ImageUploadVersion version() {
        return ImageUploadVersion.V1;
    }

    @Override
    public List<ProcessedPostImage> process(
            List<PreparedPostImage> images,
            ImageUploadFailurePoint failurePoint
    ) {
        List<ProcessedPostImage> results = new ArrayList<>();
        for (PreparedPostImage image : images) {
            results.add(processWithPermit(image, failurePoint));
        }
        return List.copyOf(results);
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
