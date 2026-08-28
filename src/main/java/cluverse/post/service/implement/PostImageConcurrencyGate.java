package cluverse.post.service.implement;

import cluverse.post.domain.ImageUploadVersion;
import org.springframework.stereotype.Component;

import java.util.concurrent.Semaphore;
import java.util.function.Supplier;

@Component
public class PostImageConcurrencyGate {

    private final Semaphore semaphore;
    private final PostImageUploadMetricsRecorder metricsRecorder;

    public PostImageConcurrencyGate(
            Semaphore postImageRemoteCallSemaphore,
            PostImageUploadMetricsRecorder metricsRecorder
    ) {
        this.semaphore = postImageRemoteCallSemaphore;
        this.metricsRecorder = metricsRecorder;
    }

    public <T> T execute(Supplier<T> operation) {
        long waitingAt = System.nanoTime();
        acquire();
        metricsRecorder.waitTime(
                ImageUploadVersion.V3,
                "semaphore",
                System.nanoTime() - waitingAt
        );
        try {
            return operation.get();
        } finally {
            semaphore.release();
        }
    }

    private void acquire() {
        try {
            semaphore.acquire();
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("이미지 처리 대기 중 요청이 중단됐습니다.", failure);
        }
    }
}
