package cluverse.post.service.implement;

import cluverse.post.client.PostImageObjectStorageClient;
import cluverse.post.client.PostImageProcessCommand;
import cluverse.post.client.PostImageProcessorClient;
import cluverse.post.domain.PostImageMetadata;
import cluverse.post.domain.ProcessedPostImage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VirtualThreadPostImageUploadProcessorTest {

    private ExecutorService executor;

    @AfterEach
    void tearDown() throws InterruptedException {
        if (executor != null) {
            executor.shutdownNow();
            executor.awaitTermination(1, TimeUnit.SECONDS);
        }
    }

    @Test
    void Virtual_Thread는_Virtual_Thread를_쓰되_Semaphore_허용량을_넘지_않는다() {
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
        VirtualThreadPostImageUploadProcessor processor = new VirtualThreadPostImageUploadProcessor(
                storage, client, mock(PostImageUploadMetricsRecorder.class), executor, new Semaphore(1));

        List<ProcessedPostImage> results = processor.process(
                List.of(image(0), image(1), image(2), image(3)));

        assertThat(results).hasSize(4);
        assertThat(maxInFlight).hasValue(1);
    }

    @Test
    void Virtual_Thread는_일부_제출이_거절되면_이미_제출한_작업을_기다린_뒤_실패한다() throws Exception {
        CountDownLatch firstTaskStarted = new CountDownLatch(1);
        CountDownLatch releaseFirstTask = new CountDownLatch(1);
        PostImageProcessorClient client = mock(PostImageProcessorClient.class);
        when(client.process(any())).thenAnswer(invocation -> {
            firstTaskStarted.countDown();
            releaseFirstTask.await(2, TimeUnit.SECONDS);
            return processed(invocation.getArgument(0));
        });
        executor = new ThreadPoolExecutor(
                1,
                1,
                0L,
                TimeUnit.MILLISECONDS,
                new SynchronousQueue<>(),
                new ThreadPoolExecutor.AbortPolicy()
        );
        VirtualThreadPostImageUploadProcessor processor = new VirtualThreadPostImageUploadProcessor(
                storageClient(), client, mock(PostImageUploadMetricsRecorder.class), executor, new Semaphore(1));

        CompletableFuture<List<ProcessedPostImage>> processing = CompletableFuture.supplyAsync(
                () -> processor.process(List.of(image(0), image(1))));

        assertThat(firstTaskStarted.await(1, TimeUnit.SECONDS)).isTrue();
        assertThat(processing).isNotDone();
        releaseFirstTask.countDown();
        assertThatThrownBy(processing::join)
                .hasCauseInstanceOf(RejectedExecutionException.class);
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
