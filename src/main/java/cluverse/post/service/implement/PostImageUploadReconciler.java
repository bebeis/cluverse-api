package cluverse.post.service.implement;

import cluverse.post.domain.PostImageUpload;
import cluverse.post.properties.PostImageUploadProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@ConditionalOnProperty(prefix = "image-upload-experiment", name = "enabled", havingValue = "true")
@Slf4j
public class PostImageUploadReconciler {

    private final PostImageUploadWriter writer;
    private final PostImageUploadStorageManager storageManager;
    private final PostImageUploadProperties properties;
    private final PostImageUploadMetricsRecorder metricsRecorder;

    public PostImageUploadReconciler(
            PostImageUploadWriter writer,
            PostImageUploadStorageManager storageManager,
            PostImageUploadProperties properties,
            PostImageUploadMetricsRecorder metricsRecorder
    ) {
        this.writer = writer;
        this.storageManager = storageManager;
        this.properties = properties;
        this.metricsRecorder = metricsRecorder;
    }

    @Scheduled(fixedDelayString = "${image-upload-experiment.cleanup-interval:30s}")
    public void reconcile() {
        cleanupCompletedStaging();
        failStalePending();
    }

    private void cleanupCompletedStaging() {
        for (PostImageUpload upload : writer.readCompletedWithStaging()) {
            try {
                if (!storageManager.deleteStaging(upload)) {
                    deferCleanup(upload, "completed_staging");
                    continue;
                }
                writer.markStagingCleaned(upload.getId());
                metricsRecorder.reconciled("completed_staging_deleted");
            } catch (RuntimeException exception) {
                log.warn("완료 이미지 staging 재조정에 실패했습니다. uploadId={}", upload.getId(), exception);
                deferCleanup(upload, "completed_staging");
            }
        }
    }

    private void failStalePending() {
        LocalDateTime threshold = LocalDateTime.now().minus(properties.stalePendingAfter());
        for (PostImageUpload upload : writer.readStalePending(threshold)) {
            if (writer.claimStalePending(upload.getId(), threshold)) {
                compensateClaimed(upload);
            }
        }
        for (PostImageUpload upload : writer.readStaleCompensating(threshold)) {
            compensateClaimed(upload);
        }
    }

    private void compensateClaimed(PostImageUpload upload) {
        try {
            if (!storageManager.compensate(upload)) {
                deferCleanup(upload, "stale_pending");
                return;
            }
            writer.completeCompensation(upload.getId(), "stale pending reconciled");
            metricsRecorder.reconciled("stale_pending_failed");
        } catch (RuntimeException exception) {
            log.warn("stale 이미지 업로드 재조정에 실패했습니다. uploadId={}", upload.getId(), exception);
            deferCleanup(upload, "stale_pending");
        }
    }

    private void deferCleanup(PostImageUpload upload, String operation) {
        try {
            writer.deferCleanupRetry(upload.getId());
        } catch (RuntimeException exception) {
            log.error("이미지 정리 재시도를 연기하지 못했습니다. uploadId={}, operation={}",
                    upload.getId(), operation, exception);
        }
        metricsRecorder.reconciled("cleanup_deferred");
    }
}
