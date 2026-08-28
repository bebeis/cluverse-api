package cluverse.post.service.implement;

import cluverse.common.exception.ExternalServiceException;
import cluverse.post.client.PostImageObjectStorageClient;
import cluverse.post.client.PostImageProcessCommand;
import cluverse.post.client.PostImageProcessorClient;
import cluverse.post.domain.ImageUploadVersion;
import cluverse.post.domain.PostImageMetadata;
import cluverse.post.domain.ProcessedPostImage;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Component
public class PostImageBatchProcessor {

    private final PostImageObjectStorageClient storageClient;
    private final PostImageProcessorClient processorClient;
    private final PostImageUploadMetricsRecorder metricsRecorder;
    private final ExecutorService executor;
    private final PostImageConcurrencyGate concurrencyGate;

    public PostImageBatchProcessor(
            PostImageObjectStorageClient storageClient,
            PostImageProcessorClient processorClient,
            PostImageUploadMetricsRecorder metricsRecorder,
            @Qualifier("postImageVirtualExecutor") ExecutorService executor,
            PostImageConcurrencyGate concurrencyGate
    ) {
        this.storageClient = storageClient;
        this.processorClient = processorClient;
        this.metricsRecorder = metricsRecorder;
        this.executor = executor;
        this.concurrencyGate = concurrencyGate;
    }

    public List<ProcessedPostImage> process(List<PreparedPostImage> images) {
        SubmittedTasks submitted = submit(images);
        // 보상 삭제가 아직 실행 중인 Lambda와 경쟁하지 않도록 모든 제출 작업의 종료를 기다린다.
        submitted.awaitAll();
        return submitted.results();
    }

    private SubmittedTasks submit(List<PreparedPostImage> images) {
        List<CompletableFuture<ProcessedPostImage>> tasks = new ArrayList<>();
        RuntimeException submissionFailure = null;
        for (PreparedPostImage image : images) {
            try {
                tasks.add(CompletableFuture.supplyAsync(() -> processWithPermit(image), executor));
            } catch (RuntimeException failure) {
                submissionFailure = failure;
                break;
            }
        }
        return new SubmittedTasks(List.copyOf(tasks), submissionFailure);
    }

    private ProcessedPostImage processWithPermit(PreparedPostImage image) {
        return concurrencyGate.execute(() -> processOne(image));
    }

    private ProcessedPostImage processOne(PreparedPostImage image) {
        storageClient.upload(image.plan().stagingKey(), image.contentType(), image.path());
        long startedAt = System.nanoTime();
        ProcessedPostImage result = processorClient.process(PostImageProcessCommand.from(image.plan()));
        metricsRecorder.remote(ImageUploadVersion.V3, System.nanoTime() - startedAt);
        return verifyStoredResult(result);
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

    private record SubmittedTasks(
            List<CompletableFuture<ProcessedPostImage>> tasks,
            RuntimeException submissionFailure
    ) {
        private void awaitAll() {
            try {
                CompletableFuture.allOf(tasks.toArray(CompletableFuture[]::new)).join();
            } catch (RuntimeException taskFailure) {
                // 제출 단계가 정상이라면 이미지 작업에서 발생한 실제 원인을 복원한다.
                if (submissionFailure == null) {
                    throw unwrap(taskFailure);
                }
            }
            // 일부 제출이 거절돼도 앞서 제출된 작업을 기다린 뒤 제출 실패를 우선 전파한다.
            if (submissionFailure != null) {
                throw submissionFailure;
            }
        }

        private List<ProcessedPostImage> results() {
            return tasks.stream().map(CompletableFuture::join).toList();
        }

        private RuntimeException unwrap(RuntimeException failure) {
            Throwable cause = failure.getCause();
            return cause instanceof RuntimeException runtimeFailure ? runtimeFailure : failure;
        }
    }
}
