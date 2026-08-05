package cluverse.post.service.implement;

import cluverse.post.domain.ImageUploadVersion;
import cluverse.post.domain.PostImageAsset;
import cluverse.post.domain.PostImageMetadata;
import cluverse.post.domain.PostImageUpload;
import cluverse.post.domain.PostImageUploadStatus;
import cluverse.post.domain.ProcessedPostImage;
import cluverse.post.repository.PostImageUploadRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import(PostImageUploadWriter.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PostImageUploadWriterConcurrencyTest {

    @Autowired
    private PostImageUploadWriter writer;

    @Autowired
    private PostImageUploadRepository repository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    @AfterEach
    void tearDown() throws InterruptedException {
        executor.shutdownNow();
        executor.awaitTermination(1, TimeUnit.SECONDS);
    }

    @Test
    void 정상_완료가_먼저_행을_점유하면_stale_보상은_완료_뒤_점유에_실패한다() throws Exception {
        PostImageUpload reserved = reserve();
        CountDownLatch completionLocked = new CountDownLatch(1);
        CountDownLatch allowCompletion = new CountDownLatch(1);
        CountDownLatch claimStarted = new CountDownLatch(1);
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);

        CompletableFuture<Void> completion = CompletableFuture.runAsync(() ->
                transaction.executeWithoutResult(status -> {
                    PostImageUpload upload = repository.findByIdForUpdate(reserved.getId()).orElseThrow();
                    completionLocked.countDown();
                    await(allowCompletion);
                    upload.complete(List.of(processed()));
                }), executor);
        assertThat(completionLocked.await(1, TimeUnit.SECONDS)).isTrue();

        CompletableFuture<Boolean> claim = CompletableFuture.supplyAsync(() -> {
            claimStarted.countDown();
            return writer.claimStalePending(reserved.getId(), LocalDateTime.now().plusMinutes(1));
        }, executor);
        assertThat(claimStarted.await(1, TimeUnit.SECONDS)).isTrue();
        assertThatThrownBy(() -> claim.get(200, TimeUnit.MILLISECONDS))
                .isInstanceOf(TimeoutException.class);

        allowCompletion.countDown();
        completion.get(2, TimeUnit.SECONDS);

        assertThat(claim.get(2, TimeUnit.SECONDS)).isFalse();
        assertThat(writer.read(reserved.getRequestId(), ImageUploadVersion.V1).orElseThrow().getStatus())
                .isEqualTo(PostImageUploadStatus.COMPLETED);
    }

    @Test
    void stale_보상이_먼저_점유하면_정상_완료는_거절된다() {
        PostImageUpload reserved = reserve();

        boolean claimed = writer.claimStalePending(
                reserved.getId(),
                LocalDateTime.now().plusMinutes(1)
        );

        assertThat(claimed).isTrue();
        assertThatThrownBy(() -> writer.complete(reserved.getId(), List.of(processed())))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("PENDING 업로드만 상태를 변경할 수 있습니다.");
        assertThat(writer.read(reserved.getRequestId(), ImageUploadVersion.V1).orElseThrow().getStatus())
                .isEqualTo(PostImageUploadStatus.COMPENSATING);
    }

    private PostImageUpload reserve() {
        return writer.reserve(
                UUID.randomUUID(),
                ImageUploadVersion.V1,
                List.of(PostImageAsset.plan(
                        0,
                        "image-uploads/request/staging/0",
                        "image-uploads/request/content/0.jpg",
                        "image-uploads/request/thumbnail/0.jpg",
                        100
                ))
        );
    }

    private ProcessedPostImage processed() {
        return new ProcessedPostImage(
                0,
                new PostImageMetadata("image-uploads/request/content/0.jpg", "image/jpeg", 1280, 720, 50),
                new PostImageMetadata("image-uploads/request/thumbnail/0.jpg", "image/jpeg", 320, 180, 20)
        );
    }

    private void await(CountDownLatch latch) {
        try {
            if (!latch.await(2, TimeUnit.SECONDS)) {
                throw new IllegalStateException("동시성 테스트 대기 시간이 초과됐습니다.");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("동시성 테스트가 중단됐습니다.", exception);
        }
    }
}
