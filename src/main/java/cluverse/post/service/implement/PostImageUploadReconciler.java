package cluverse.post.service.implement;

import cluverse.post.domain.PostImageUpload;
import cluverse.post.properties.PostImageUploadProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Slf4j
public class PostImageUploadReconciler {

    private final PostImageUploadRecoveryStore recoveryStore;
    private final PostImageUploadRecovery recovery;
    private final PostImageStagingCleanup stagingCleanup;
    private final PostImageUploadProperties properties;
    private final PostImageUploadMetricsRecorder metricsRecorder;

    public PostImageUploadReconciler(
            PostImageUploadRecoveryStore recoveryStore,
            PostImageUploadRecovery recovery,
            PostImageStagingCleanup stagingCleanup,
            PostImageUploadProperties properties,
            PostImageUploadMetricsRecorder metricsRecorder
    ) {
        this.recoveryStore = recoveryStore;
        this.recovery = recovery;
        this.stagingCleanup = stagingCleanup;
        this.properties = properties;
        this.metricsRecorder = metricsRecorder;
    }

    @Scheduled(fixedDelayString = "${post-image-upload.cleanup-interval:30s}")
    public void reconcile() {
        // 각 작업은 실패한 항목을 뒤로 미루므로 한 종류의 정리 실패가 다음 작업을 막지 않는다.
        cleanupCompletedStaging();
        failStalePending();
        cleanupUnclaimedCompleted();
    }

    private void cleanupUnclaimedCompleted() {
        LocalDateTime threshold = LocalDateTime.now().minus(properties.unclaimedAfter());
        for (PostImageUpload upload : recoveryStore.readUnclaimedCompleted(threshold)) {
            if (!recoveryStore.claimUnclaimedCompleted(upload.getId(), threshold)) {
                continue;
            }
            PostImageCleanupOutcome outcome = recovery.compensateClaimed(
                    upload, "unclaimed completed upload expired");
            if (outcome == PostImageCleanupOutcome.COMPLETED) {
                metricsRecorder.reconciled("unclaimed_completed_deleted");
            } else {
                deferCleanup(upload, "unclaimed_completed");
            }
        }
    }

    private void cleanupCompletedStaging() {
        for (PostImageUpload upload : recoveryStore.readCompletedWithStaging()) {
            if (stagingCleanup.clean(upload) == PostImageCleanupOutcome.COMPLETED) {
                metricsRecorder.reconciled("completed_staging_deleted");
            } else {
                deferCleanup(upload, "completed_staging");
            }
        }
    }

    private void failStalePending() {
        LocalDateTime threshold = LocalDateTime.now().minus(properties.stalePendingAfter());
        for (PostImageUpload upload : recoveryStore.readStalePending(threshold)) {
            if (recoveryStore.claimStalePending(upload.getId(), threshold)) {
                compensateClaimed(upload);
            }
        }
        for (PostImageUpload upload : recoveryStore.readStaleCompensating(threshold)) {
            compensateClaimed(upload);
        }
    }

    private void compensateClaimed(PostImageUpload upload) {
        if (recovery.compensateClaimed(upload, "stale pending reconciled")
                == PostImageCleanupOutcome.COMPLETED) {
            metricsRecorder.reconciled("stale_pending_failed");
        } else {
            deferCleanup(upload, "stale_pending");
        }
    }

    private void deferCleanup(PostImageUpload upload, String operation) {
        try {
            recoveryStore.deferCleanupRetry(upload.getId());
        } catch (RuntimeException exception) {
            log.error("이미지 정리 재시도를 연기하지 못했습니다. uploadId={}, operation={}",
                    upload.getId(), operation, exception);
        }
        metricsRecorder.reconciled("cleanup_deferred");
    }
}
