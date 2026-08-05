package cluverse.post.service.implement;

import cluverse.post.domain.PostImageUpload;
import cluverse.post.properties.PostImageUploadProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PostImageUploadReconcilerTest {

    @Test
    void 정리_실패_레코드는_뒤로_보내_다음_배치가_진행되게_한다() {
        PostImageUploadWriter writer = mock(PostImageUploadWriter.class);
        PostImageUploadStorageManager storageManager = mock(PostImageUploadStorageManager.class);
        PostImageUploadProperties properties = mock(PostImageUploadProperties.class);
        PostImageUploadMetricsRecorder metrics = mock(PostImageUploadMetricsRecorder.class);
        PostImageUpload completed = upload(1L);
        PostImageUpload pending = upload(2L);
        when(writer.readCompletedWithStaging()).thenReturn(List.of(completed));
        when(properties.stalePendingAfter()).thenReturn(Duration.ofMinutes(3));
        when(writer.readStalePending(any())).thenReturn(List.of(pending));
        when(writer.readStaleCompensating(any())).thenReturn(List.of());
        when(writer.claimStalePending(eq(2L), any())).thenReturn(true);
        when(storageManager.deleteStaging(completed)).thenReturn(false);
        when(storageManager.compensate(pending)).thenReturn(false);
        PostImageUploadReconciler reconciler = new PostImageUploadReconciler(
                writer, storageManager, properties, metrics);

        reconciler.reconcile();

        verify(writer).deferCleanupRetry(1L);
        verify(writer).deferCleanupRetry(2L);
    }

    @Test
    void stale_PENDING_점유에_실패하면_보상하지_않는다() {
        PostImageUploadWriter writer = mock(PostImageUploadWriter.class);
        PostImageUploadStorageManager storageManager = mock(PostImageUploadStorageManager.class);
        PostImageUploadProperties properties = mock(PostImageUploadProperties.class);
        PostImageUpload pending = upload(1L);
        when(properties.stalePendingAfter()).thenReturn(Duration.ofMinutes(3));
        when(writer.readCompletedWithStaging()).thenReturn(List.of());
        when(writer.readStalePending(any())).thenReturn(List.of(pending));
        when(writer.readStaleCompensating(any())).thenReturn(List.of());
        when(writer.claimStalePending(eq(1L), any())).thenReturn(false);
        PostImageUploadReconciler reconciler = new PostImageUploadReconciler(
                writer,
                storageManager,
                properties,
                mock(PostImageUploadMetricsRecorder.class)
        );

        reconciler.reconcile();

        verify(storageManager, never()).compensate(any());
        verify(writer, never()).completeCompensation(any(), any());
    }

    @Test
    void 점유한_stale_PENDING은_보상한_뒤_FAILED로_확정한다() {
        PostImageUploadWriter writer = mock(PostImageUploadWriter.class);
        PostImageUploadStorageManager storageManager = mock(PostImageUploadStorageManager.class);
        PostImageUploadProperties properties = mock(PostImageUploadProperties.class);
        PostImageUpload pending = upload(1L);
        when(properties.stalePendingAfter()).thenReturn(Duration.ofMinutes(3));
        when(writer.readCompletedWithStaging()).thenReturn(List.of());
        when(writer.readStalePending(any())).thenReturn(List.of(pending));
        when(writer.readStaleCompensating(any())).thenReturn(List.of());
        when(writer.claimStalePending(eq(1L), any())).thenReturn(true);
        when(storageManager.compensate(pending)).thenReturn(true);
        PostImageUploadReconciler reconciler = new PostImageUploadReconciler(
                writer,
                storageManager,
                properties,
                mock(PostImageUploadMetricsRecorder.class)
        );

        reconciler.reconcile();

        verify(writer).completeCompensation(1L, "stale pending reconciled");
    }

    private PostImageUpload upload(Long id) {
        PostImageUpload upload = mock(PostImageUpload.class);
        when(upload.getId()).thenReturn(id);
        return upload;
    }
}
