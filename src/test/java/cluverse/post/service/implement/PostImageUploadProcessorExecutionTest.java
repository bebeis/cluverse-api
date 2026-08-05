package cluverse.post.service.implement;

import cluverse.post.client.PostImageObjectStorageClient;
import cluverse.post.client.PostImageProcessCommand;
import cluverse.post.client.PostImageProcessorClient;
import cluverse.post.domain.PostImageMetadata;
import cluverse.post.domain.ProcessedPostImage;
import cluverse.post.service.request.ImageUploadFailurePoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PostImageUploadProcessorExecutionTest {

    private ExecutorService executor;

    @AfterEach
    void tearDown() throws InterruptedException {
        if (executor != null) {
            executor.shutdownNow();
            executor.awaitTermination(1, TimeUnit.SECONDS);
        }
    }

    @Test
    void V1은_한_이미지의_외부_호출이_끝난_뒤_다음_이미지를_처리한다() {
        PostImageObjectStorageClient storage = storageClient();
        PostImageProcessorClient client = processorClient();
        PostImageUploadProcessorV1 processor = new PostImageUploadProcessorV1(
                storage, client, mock(PostImageUploadMetricsRecorder.class), new Semaphore(2));
        PreparedPostImage first = image(0);
        PreparedPostImage second = image(1);

        processor.process(List.of(first, second), ImageUploadFailurePoint.NONE);

        var order = inOrder(storage, client);
        order.verify(storage).upload(first.command().stagingKey(), first.contentType(), first.path());
        order.verify(client).process(first.command());
        order.verify(storage).upload(second.command().stagingKey(), second.contentType(), second.path());
        order.verify(client).process(second.command());
    }

    @Test
    void V2는_CompletableFuture로_외부_호출을_겹쳐_실행한다() throws Exception {
        // 순차 실행이면 첫 작업이 barrier에서 timeout되므로 두 작업의 실제 겹침을 검증한다.
        CyclicBarrier barrier = new CyclicBarrier(2);
        PostImageObjectStorageClient storage = storageClient();
        PostImageProcessorClient client = processorClient(barrier);
        executor = Executors.newFixedThreadPool(2);
        PostImageUploadProcessorV2 processor = new PostImageUploadProcessorV2(
                storage, client, mock(PostImageUploadMetricsRecorder.class), executor);

        List<ProcessedPostImage> results = processor.process(
                List.of(image(0), image(1)), ImageUploadFailurePoint.NONE);

        assertThat(results).hasSize(2);
    }

    @Test
    void V2는_executor가_제출을_거절하면_호출자에게_전파한다() {
        executor = Executors.newSingleThreadExecutor();
        executor.shutdown();
        PostImageUploadProcessorV2 processor = new PostImageUploadProcessorV2(
                storageClient(),
                processorClient(),
                mock(PostImageUploadMetricsRecorder.class),
                executor
        );

        assertThatThrownBy(() -> processor.process(List.of(image(0)), ImageUploadFailurePoint.NONE))
                .isInstanceOf(RejectedExecutionException.class);
    }

    @Test
    void V2는_비동기_작업_실패를_호출자에게_전파한다() {
        PostImageProcessorClient client = mock(PostImageProcessorClient.class);
        when(client.process(any())).thenThrow(new IllegalStateException("remote failure"));
        executor = Executors.newFixedThreadPool(2);
        PostImageUploadProcessorV2 processor = new PostImageUploadProcessorV2(
                storageClient(), client, mock(PostImageUploadMetricsRecorder.class), executor);

        assertThatThrownBy(() -> processor.process(List.of(image(0), image(1)), ImageUploadFailurePoint.NONE))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("remote failure");
    }

    @Test
    void V3는_Virtual_Thread를_쓰되_Semaphore_허용량을_넘지_않는다() {
        AtomicInteger inFlight = new AtomicInteger();
        AtomicInteger maxInFlight = new AtomicInteger();
        PostImageObjectStorageClient storage = storageClient();
        PostImageProcessorClient client = mock(PostImageProcessorClient.class);
        when(client.process(any())).thenAnswer(invocation -> {
            int current = inFlight.incrementAndGet();
            maxInFlight.accumulateAndGet(current, Math::max);
            try {
                Thread.sleep(30);
                return processed(invocation.getArgument(0));
            } finally {
                inFlight.decrementAndGet();
            }
        });
        executor = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().factory());
        PostImageUploadProcessorV3 processor = new PostImageUploadProcessorV3(
                storage, client, mock(PostImageUploadMetricsRecorder.class), executor, new Semaphore(1));

        List<ProcessedPostImage> results = processor.process(
                List.of(image(0), image(1), image(2), image(3)), ImageUploadFailurePoint.NONE);

        assertThat(results).hasSize(4);
        assertThat(maxInFlight).hasValue(1);
    }

    private PreparedPostImage image(int order) {
        String prefix = "request/" + order;
        PostImageProcessCommand command = new PostImageProcessCommand(
                UUID.randomUUID(), order, prefix + "/staging", prefix + "/content", prefix + "/thumbnail", "p1");
        return new PreparedPostImage(Path.of(prefix), "image/jpeg", 100, command);
    }

    private PostImageObjectStorageClient storageClient() {
        PostImageObjectStorageClient storage = mock(PostImageObjectStorageClient.class);
        when(storage.size(any())).thenReturn(50L);
        return storage;
    }

    private PostImageProcessorClient processorClient() {
        return processorClient(null);
    }

    private PostImageProcessorClient processorClient(CyclicBarrier barrier) {
        PostImageProcessorClient client = mock(PostImageProcessorClient.class);
        when(client.process(any())).thenAnswer(invocation -> {
            PostImageProcessCommand command = invocation.getArgument(0);
            if (barrier != null) {
                barrier.await(2, TimeUnit.SECONDS);
            }
            return processed(command);
        });
        return client;
    }

    private ProcessedPostImage processed(PostImageProcessCommand command) {
        return new ProcessedPostImage(
                command.displayOrder(),
                new PostImageMetadata(command.contentKey(), "image/jpeg", 1280, 720, 50),
                new PostImageMetadata(command.thumbnailKey(), "image/jpeg", 320, 180, 20)
        );
    }
}
