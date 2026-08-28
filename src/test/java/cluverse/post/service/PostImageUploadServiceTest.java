package cluverse.post.service;

import cluverse.post.domain.ImageUploadVersion;
import cluverse.post.domain.PostImageAsset;
import cluverse.post.domain.PostImageUpload;
import cluverse.post.exception.PostImageUploadTimeoutException;
import cluverse.post.service.implement.PostImageUploadMetricsRecorder;
import cluverse.post.service.implement.PostImageUploadPreparer;
import cluverse.post.service.implement.PostImageBatchProcessor;
import cluverse.post.service.implement.PostImageUploadReservation;
import cluverse.post.service.implement.PostImageUploadReservationResult;
import cluverse.post.service.implement.PostImageUploadRecovery;
import cluverse.post.service.implement.PostImageUploadRecoveryStore;
import cluverse.post.service.implement.PostImageStagingCleanup;
import cluverse.post.service.implement.PostImageUploadStorageManager;
import cluverse.post.service.implement.PostImageUploadTemporaryFileCleaner;
import cluverse.post.service.implement.PostImageUploadWriter;
import cluverse.post.service.implement.PreparedPostImageUpload;
import cluverse.post.service.request.PostImageUploadRequest;
import cluverse.post.service.response.PostImageUploadResponse;
import org.junit.jupiter.api.Test;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Semaphore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PostImageUploadServiceTest {

    @Test
    void 준비한_이미지를_Virtual_Thread_프로세서로_처리한다() {
        PostImageUploadPreparer preparer = mock(PostImageUploadPreparer.class);
        PostImageUploadReservation reservation = mock(PostImageUploadReservation.class);
        PostImageUploadWriter writer = mock(PostImageUploadWriter.class);
        PostImageUploadStorageManager storageManager = mock(PostImageUploadStorageManager.class);
        PostImageUploadMetricsRecorder metricsRecorder = metricsRecorder();
        PostImageBatchProcessor processor = processor();
        PostImageUploadRequest request = new PostImageUploadRequest(
                UUID.randomUUID(),
                List.of(new MockMultipartFile("images", "sample.jpg", "image/jpeg", new byte[]{1}))
        );
        PostImageAsset plan = PostImageAsset.plan(0, "staging/a", "content/a.jpg", "thumbnail/a.jpg", 1);
        PreparedPostImageUpload prepared = new PreparedPostImageUpload(
                List.of(), List.of(plan));
        PostImageUpload reserved = PostImageUpload.reserve(
                request.requestId(), ImageUploadVersion.V3, List.of(plan));
        PostImageUpload completed = PostImageUpload.completed(
                request.requestId(), ImageUploadVersion.V3,
                List.of(PostImageAsset.completedOriginal(0, "content/a.jpg", "image/jpeg", 1))
        );
        when(preparer.prepare(ImageUploadVersion.V3, request)).thenReturn(prepared);
        when(writer.read(request.requestId(), ImageUploadVersion.V3)).thenReturn(Optional.empty());
        when(reservation.reserve(isNull(), eq(request.requestId()), eq(ImageUploadVersion.V3),
                eq(prepared.assets())))
                .thenReturn(new PostImageUploadReservationResult.Created(reserved));
        when(processor.process(prepared.images())).thenReturn(List.of());
        when(writer.complete(isNull(), any())).thenReturn(completed);
        when(storageManager.createImageUrl("content/a.jpg"))
                .thenReturn("https://images.example.com/content/a.jpg");
        PostImageUploadService service = new PostImageUploadService(
                preparer, reservation, writer, storageManager, metricsRecorder, processor,
                recovery(storageManager, writer),
                new PostImageStagingCleanup(storageManager, writer));

        PostImageUploadResponse response = service.upload(request);

        assertThat(response.status()).isEqualTo("COMPLETED");
        assertThat(response.images().getFirst().contentUrl())
                .isEqualTo("https://images.example.com/content/a.jpg");
        verify(processor).process(prepared.images());
        verify(metricsRecorder).request(eq(ImageUploadVersion.V3), eq("success"), anyLong());
    }

    @Test
    void 완료된_requestId는_기존_결과를_반환하고_idempotent로_측정한다() {
        PostImageUploadPreparer preparer = mock(PostImageUploadPreparer.class);
        PostImageUploadReservation reservation = mock(PostImageUploadReservation.class);
        PostImageUploadWriter writer = mock(PostImageUploadWriter.class);
        PostImageUploadStorageManager storageManager = mock(PostImageUploadStorageManager.class);
        PostImageUploadMetricsRecorder metricsRecorder = metricsRecorder();
        PostImageBatchProcessor processor = processor();
        UUID requestId = UUID.randomUUID();
        PostImageUploadRequest request = request(requestId);
        PostImageUpload completed = PostImageUpload.completed(
                requestId,
                ImageUploadVersion.V3,
                List.of(PostImageAsset.completedOriginal(0, "content/a.jpg", "image/jpeg", 1))
        );
        when(writer.read(requestId, ImageUploadVersion.V3)).thenReturn(Optional.of(completed));
        when(storageManager.createImageUrl("content/a.jpg"))
                .thenReturn("https://images.example.com/content/a.jpg");
        PostImageUploadService service = new PostImageUploadService(
                preparer, reservation, writer, storageManager, metricsRecorder, processor,
                recovery(storageManager, writer),
                new PostImageStagingCleanup(storageManager, writer));

        PostImageUploadResponse response = service.upload(request);

        assertThat(response.status()).isEqualTo("COMPLETED");
        verify(metricsRecorder).request(eq(ImageUploadVersion.V3), eq("idempotent"), anyLong());
        verify(preparer, never()).prepare(any(), any());
    }

    @Test
    void 진행_중인_requestId의_거절도_실패_메트릭에_포함한다() {
        PostImageUploadPreparer preparer = mock(PostImageUploadPreparer.class);
        PostImageUploadReservation reservation = mock(PostImageUploadReservation.class);
        PostImageUploadWriter writer = mock(PostImageUploadWriter.class);
        PostImageUploadStorageManager storageManager = mock(PostImageUploadStorageManager.class);
        PostImageUploadMetricsRecorder metricsRecorder = metricsRecorder();
        PostImageBatchProcessor processor = processor();
        UUID requestId = UUID.randomUUID();
        PostImageUploadRequest request = new PostImageUploadRequest(
                requestId,
                List.of(new MockMultipartFile("images", "sample.jpg", "image/jpeg", new byte[]{1}))
        );
        PostImageUpload pending = PostImageUpload.reserve(
                requestId,
                ImageUploadVersion.V3,
                List.of(PostImageAsset.plan(0, "staging/a", "content/a.jpg", "thumbnail/a.jpg", 1))
        );
        when(writer.read(requestId, ImageUploadVersion.V3)).thenReturn(Optional.of(pending));
        PostImageUploadService service = new PostImageUploadService(
                preparer,
                reservation,
                writer,
                storageManager,
                metricsRecorder,
                processor,
                recovery(storageManager, writer),
                new PostImageStagingCleanup(storageManager, writer)
        );

        assertThatThrownBy(() -> service.upload(request))
                .isInstanceOf(cluverse.common.exception.BadRequestException.class);

        verify(metricsRecorder).request(eq(ImageUploadVersion.V3), eq("failure"), anyLong());
        verify(preparer, never()).prepare(any(), any());
    }

    @Test
    void 외부_처리_timeout은_늦은_Lambda와_경쟁하지_않도록_즉시_보상하지_않는다() {
        PostImageUploadPreparer preparer = mock(PostImageUploadPreparer.class);
        PostImageUploadReservation reservation = mock(PostImageUploadReservation.class);
        PostImageUploadWriter writer = mock(PostImageUploadWriter.class);
        PostImageUploadStorageManager storageManager = mock(PostImageUploadStorageManager.class);
        PostImageUploadMetricsRecorder metricsRecorder = metricsRecorder();
        PostImageBatchProcessor processor = processor();
        UUID requestId = UUID.randomUUID();
        PostImageUploadRequest request = new PostImageUploadRequest(
                requestId,
                List.of(new MockMultipartFile("images", "sample.jpg", "image/jpeg", new byte[]{1}))
        );
        PostImageAsset plan = PostImageAsset.plan(0, "staging/a", "content/a.jpg", "thumbnail/a.jpg", 1);
        PreparedPostImageUpload prepared = new PreparedPostImageUpload(
                List.of(), List.of(plan));
        PostImageUpload reserved = PostImageUpload.reserve(requestId, ImageUploadVersion.V3, List.of(plan));
        when(writer.read(requestId, ImageUploadVersion.V3)).thenReturn(Optional.empty());
        when(preparer.prepare(ImageUploadVersion.V3, request)).thenReturn(prepared);
        when(reservation.reserve(isNull(), eq(requestId), eq(ImageUploadVersion.V3),
                eq(prepared.assets())))
                .thenReturn(new PostImageUploadReservationResult.Created(reserved));
        when(processor.process(prepared.images()))
                .thenThrow(new PostImageUploadTimeoutException("timeout", new IllegalStateException()));
        PostImageUploadService service = new PostImageUploadService(
                preparer, reservation, writer, storageManager, metricsRecorder, processor,
                recovery(storageManager, writer),
                new PostImageStagingCleanup(storageManager, writer));

        assertThatThrownBy(() -> service.upload(request))
                .isInstanceOf(PostImageUploadTimeoutException.class);

        verify(metricsRecorder).request(eq(ImageUploadVersion.V3), eq("timeout"), anyLong());
        verify(storageManager, never()).deleteAll(any());
        verify(writer, never()).fail(any(), any());
    }

    @Test
    void 예약_이후_일반_실패는_S3_객체를_삭제한_뒤_FAILED로_기록한다() {
        PostImageUploadPreparer preparer = mock(PostImageUploadPreparer.class);
        PostImageUploadReservation reservation = mock(PostImageUploadReservation.class);
        PostImageUploadWriter writer = mock(PostImageUploadWriter.class);
        PostImageUploadStorageManager storageManager = mock(PostImageUploadStorageManager.class);
        PostImageUploadMetricsRecorder metricsRecorder = metricsRecorder();
        PostImageBatchProcessor processor = processor();
        UUID requestId = UUID.randomUUID();
        PostImageUploadRequest request = request(requestId);
        PostImageAsset plan = plan();
        PreparedPostImageUpload prepared = prepared(plan);
        PostImageUpload reserved = PostImageUpload.reserve(requestId, ImageUploadVersion.V3, List.of(plan));
        RuntimeException failure = new IllegalStateException("boom");
        when(writer.read(requestId, ImageUploadVersion.V3)).thenReturn(Optional.empty());
        when(preparer.prepare(ImageUploadVersion.V3, request)).thenReturn(prepared);
        when(reservation.reserve(isNull(), eq(requestId), eq(ImageUploadVersion.V3), eq(prepared.assets())))
                .thenReturn(new PostImageUploadReservationResult.Created(reserved));
        when(processor.process(prepared.images())).thenThrow(failure);
        PostImageUploadService service = new PostImageUploadService(
                preparer, reservation, writer, storageManager, metricsRecorder, processor,
                recovery(storageManager, writer),
                new PostImageStagingCleanup(storageManager, writer));

        assertThatThrownBy(() -> service.upload(request)).isSameAs(failure);

        var ordered = org.mockito.Mockito.inOrder(storageManager, writer);
        ordered.verify(storageManager).deleteAll(reserved);
        ordered.verify(writer).fail(reserved.getId(), "boom");
    }

    @Test
    void S3_보상에_실패하면_PENDING_기록을_재조정_기준점으로_남긴다() {
        PostImageUploadPreparer preparer = mock(PostImageUploadPreparer.class);
        PostImageUploadReservation reservation = mock(PostImageUploadReservation.class);
        PostImageUploadWriter writer = mock(PostImageUploadWriter.class);
        PostImageUploadStorageManager storageManager = mock(PostImageUploadStorageManager.class);
        PostImageUploadMetricsRecorder metricsRecorder = metricsRecorder();
        PostImageBatchProcessor processor = processor();
        UUID requestId = UUID.randomUUID();
        PostImageUploadRequest request = request(requestId);
        PostImageAsset plan = plan();
        PreparedPostImageUpload prepared = prepared(plan);
        PostImageUpload reserved = PostImageUpload.reserve(requestId, ImageUploadVersion.V3, List.of(plan));
        RuntimeException failure = new IllegalStateException("boom");
        when(writer.read(requestId, ImageUploadVersion.V3)).thenReturn(Optional.empty());
        when(preparer.prepare(ImageUploadVersion.V3, request)).thenReturn(prepared);
        when(reservation.reserve(isNull(), eq(requestId), eq(ImageUploadVersion.V3), eq(prepared.assets())))
                .thenReturn(new PostImageUploadReservationResult.Created(reserved));
        when(processor.process(prepared.images())).thenThrow(failure);
        doThrow(new IllegalStateException("delete failed")).when(storageManager).deleteAll(reserved);
        PostImageUploadService service = new PostImageUploadService(
                preparer, reservation, writer, storageManager, metricsRecorder, processor,
                recovery(storageManager, writer),
                new PostImageStagingCleanup(storageManager, writer));

        assertThatThrownBy(() -> service.upload(request)).isSameAs(failure);

        verify(writer, never()).fail(any(), any());
    }

    @Test
    void 완료_후_staging_정리_실패는_성공_응답을_바꾸지_않는다() {
        PostImageUploadPreparer preparer = mock(PostImageUploadPreparer.class);
        PostImageUploadReservation reservation = mock(PostImageUploadReservation.class);
        PostImageUploadWriter writer = mock(PostImageUploadWriter.class);
        PostImageUploadStorageManager storageManager = mock(PostImageUploadStorageManager.class);
        PostImageUploadMetricsRecorder metricsRecorder = metricsRecorder();
        PostImageBatchProcessor processor = processor();
        UUID requestId = UUID.randomUUID();
        PostImageUploadRequest request = request(requestId);
        PostImageAsset plan = plan();
        PreparedPostImageUpload prepared = prepared(plan);
        PostImageUpload reserved = PostImageUpload.reserve(requestId, ImageUploadVersion.V3, List.of(plan));
        PostImageUpload completed = PostImageUpload.completed(
                requestId,
                ImageUploadVersion.V3,
                List.of(PostImageAsset.completedOriginal(0, "content/a.jpg", "image/jpeg", 1))
        );
        when(writer.read(requestId, ImageUploadVersion.V3)).thenReturn(Optional.empty());
        when(preparer.prepare(ImageUploadVersion.V3, request)).thenReturn(prepared);
        when(reservation.reserve(isNull(), eq(requestId), eq(ImageUploadVersion.V3), eq(prepared.assets())))
                .thenReturn(new PostImageUploadReservationResult.Created(reserved));
        when(processor.process(prepared.images())).thenReturn(List.of());
        when(writer.complete(reserved.getId(), List.of())).thenReturn(completed);
        doThrow(new IllegalStateException("delete failed")).when(storageManager).deleteStaging(completed);
        when(storageManager.createImageUrl("content/a.jpg"))
                .thenReturn("https://images.example.com/content/a.jpg");
        PostImageUploadService service = new PostImageUploadService(
                preparer, reservation, writer, storageManager, metricsRecorder, processor,
                recovery(storageManager, writer),
                new PostImageStagingCleanup(storageManager, writer));

        PostImageUploadResponse response = service.upload(request);

        assertThat(response.status()).isEqualTo("COMPLETED");
        verify(writer, never()).markStagingCleaned(any());
    }

    private PostImageUploadRequest request(UUID requestId) {
        return new PostImageUploadRequest(
                requestId,
                List.of(new MockMultipartFile("images", "sample.jpg", "image/jpeg", new byte[]{1}))
        );
    }

    private PostImageAsset plan() {
        return PostImageAsset.plan(0, "staging/a", "content/a.jpg", "thumbnail/a.jpg", 1);
    }

    private PreparedPostImageUpload prepared(PostImageAsset plan) {
        return new PreparedPostImageUpload(
                List.of(), List.of(plan));
    }

    private PostImageBatchProcessor processor() {
        return mock(PostImageBatchProcessor.class);
    }

    private PostImageUploadMetricsRecorder metricsRecorder() {
        return spy(new PostImageUploadMetricsRecorder(new SimpleMeterRegistry(), new Semaphore(1)));
    }

    private PostImageUploadRecovery recovery(
            PostImageUploadStorageManager storageManager,
            PostImageUploadWriter writer
    ) {
        return new PostImageUploadRecovery(
                storageManager, writer, mock(PostImageUploadRecoveryStore.class));
    }
}
