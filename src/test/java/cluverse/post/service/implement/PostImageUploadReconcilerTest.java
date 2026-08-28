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
        PostImageUploadRecoveryStore store = mock(PostImageUploadRecoveryStore.class);
        PostImageUploadRecovery recovery = mock(PostImageUploadRecovery.class);
        PostImageStagingCleanup stagingCleanup = mock(PostImageStagingCleanup.class);
        PostImageUpload completed = upload(1L);
        PostImageUpload pending = upload(2L);
        when(store.readCompletedWithStaging()).thenReturn(List.of(completed));
        when(store.readStalePending(any())).thenReturn(List.of(pending));
        when(store.readStaleCompensating(any())).thenReturn(List.of());
        when(store.readUnclaimedCompleted(any())).thenReturn(List.of());
        when(store.claimStalePending(eq(2L), any())).thenReturn(true);
        when(stagingCleanup.clean(completed)).thenReturn(PostImageCleanupOutcome.DEFERRED);
        when(recovery.compensateClaimed(pending, "stale pending reconciled"))
                .thenReturn(PostImageCleanupOutcome.DEFERRED);
        PostImageUploadReconciler reconciler = new PostImageUploadReconciler(
                store, recovery, stagingCleanup, properties(), mock(PostImageUploadMetricsRecorder.class));

        reconciler.reconcile();

        verify(store).deferCleanupRetry(1L);
        verify(store).deferCleanupRetry(2L);
    }

    @Test
    void stale_PENDING_점유에_실패하면_보상하지_않는다() {
        PostImageUploadRecoveryStore store = mock(PostImageUploadRecoveryStore.class);
        PostImageUploadRecovery recovery = mock(PostImageUploadRecovery.class);
        PostImageStagingCleanup stagingCleanup = mock(PostImageStagingCleanup.class);
        PostImageUpload pending = upload(1L);
        when(store.readCompletedWithStaging()).thenReturn(List.of());
        when(store.readStalePending(any())).thenReturn(List.of(pending));
        when(store.readStaleCompensating(any())).thenReturn(List.of());
        when(store.readUnclaimedCompleted(any())).thenReturn(List.of());
        when(store.claimStalePending(eq(1L), any())).thenReturn(false);
        PostImageUploadReconciler reconciler = new PostImageUploadReconciler(
                store, recovery, stagingCleanup, properties(), mock(PostImageUploadMetricsRecorder.class));

        reconciler.reconcile();

        verify(recovery, never()).compensateClaimed(any(), any());
    }

    @Test
    void 점유한_stale_PENDING은_보상한_뒤_FAILED로_확정한다() {
        PostImageUploadRecoveryStore store = mock(PostImageUploadRecoveryStore.class);
        PostImageUploadRecovery recovery = mock(PostImageUploadRecovery.class);
        PostImageStagingCleanup stagingCleanup = mock(PostImageStagingCleanup.class);
        PostImageUploadMetricsRecorder metrics = mock(PostImageUploadMetricsRecorder.class);
        PostImageUpload pending = upload(1L);
        when(store.readCompletedWithStaging()).thenReturn(List.of());
        when(store.readStalePending(any())).thenReturn(List.of(pending));
        when(store.readStaleCompensating(any())).thenReturn(List.of());
        when(store.readUnclaimedCompleted(any())).thenReturn(List.of());
        when(store.claimStalePending(eq(1L), any())).thenReturn(true);
        when(recovery.compensateClaimed(pending, "stale pending reconciled"))
                .thenReturn(PostImageCleanupOutcome.COMPLETED);
        PostImageUploadReconciler reconciler = new PostImageUploadReconciler(
                store, recovery, stagingCleanup, properties(), metrics);

        reconciler.reconcile();

        verify(recovery).compensateClaimed(pending, "stale pending reconciled");
        verify(metrics).reconciled("stale_pending_failed");
    }

    private PostImageUploadProperties properties() {
        PostImageUploadProperties properties = mock(PostImageUploadProperties.class);
        when(properties.stalePendingAfter()).thenReturn(Duration.ofMinutes(3));
        when(properties.unclaimedAfter()).thenReturn(Duration.ofHours(24));
        return properties;
    }

    private PostImageUpload upload(Long id) {
        PostImageUpload upload = mock(PostImageUpload.class);
        when(upload.getId()).thenReturn(id);
        return upload;
    }
}
