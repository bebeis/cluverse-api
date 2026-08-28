package cluverse.post.service.implement;

import cluverse.post.domain.PostImageUpload;
import cluverse.post.exception.PostImageUploadTimeoutException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PostImageUploadRecovery {

    private final PostImageUploadStorageManager storageManager;
    private final PostImageUploadWriter writer;
    private final PostImageUploadRecoveryStore recoveryStore;

    public void afterProcessingFailure(PostImageUpload upload, RuntimeException failure) {
        if (failure instanceof PostImageUploadTimeoutException) {
            // Lambda가 늦게 완료될 수 있으므로 예정 key를 즉시 삭제하지 않는다.
            return;
        }
        try {
            // 객체 삭제가 끝난 뒤에만 FAILED를 기록한다. 중간 실패 시 PENDING이 복구 기준점으로 남는다.
            storageManager.deleteAll(upload);
            writer.fail(upload.getId(), failure.getMessage());
        } catch (RuntimeException recoveryFailure) {
            log.warn("이미지 업로드 보상을 완료하지 못했습니다. uploadId={}", upload.getId(), recoveryFailure);
        }
    }

    public PostImageCleanupOutcome compensateClaimed(PostImageUpload upload, String reason) {
        try {
            storageManager.deleteAll(upload);
            recoveryStore.completeCompensation(upload.getId(), reason);
            return PostImageCleanupOutcome.COMPLETED;
        } catch (RuntimeException failure) {
            log.warn("이미지 업로드 재조정을 완료하지 못했습니다. uploadId={}", upload.getId(), failure);
            return PostImageCleanupOutcome.DEFERRED;
        }
    }
}
