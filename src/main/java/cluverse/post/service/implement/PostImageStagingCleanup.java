package cluverse.post.service.implement;

import cluverse.post.domain.PostImageUpload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PostImageStagingCleanup {

    private final PostImageUploadStorageManager storageManager;
    private final PostImageUploadWriter writer;

    public PostImageCleanupOutcome clean(PostImageUpload completed) {
        try {
            storageManager.deleteStaging(completed);
            writer.markStagingCleaned(completed.getId());
            return PostImageCleanupOutcome.COMPLETED;
        } catch (RuntimeException failure) {
            // 게시 결과는 이미 COMPLETED 상태이므로 성공을 되돌리지 않고 scheduler 재시도 대상으로 남긴다.
            log.warn("완료된 이미지 staging 정리를 연기합니다. uploadId={}", completed.getId(), failure);
            return PostImageCleanupOutcome.DEFERRED;
        }
    }
}
