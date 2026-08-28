package cluverse.post.service.implement;

import cluverse.post.domain.PostImageUpload;
import cluverse.post.domain.PostImageUploadStatus;
import cluverse.post.repository.PostImageUploadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PostImageUploadRecoveryStore {

    private final PostImageUploadRepository repository;

    @Transactional
    public boolean claimStalePending(Long uploadId, LocalDateTime threshold) {
        return repository.claimStalePending(
                uploadId,
                threshold,
                PostImageUploadStatus.PENDING,
                PostImageUploadStatus.COMPENSATING
        ) == 1;
    }

    @Transactional
    public void completeCompensation(Long uploadId, String reason) {
        readById(uploadId).completeCompensation(reason);
    }

    @Transactional
    public void deferCleanupRetry(Long uploadId) {
        if (repository.touchUpdatedAt(uploadId) != 1) {
            throw new IllegalStateException("정리 재시도 대상을 찾을 수 없습니다.");
        }
    }

    @Transactional(readOnly = true)
    public List<PostImageUpload> readStalePending(LocalDateTime threshold) {
        return repository.findTop100ByStatusAndUpdatedAtBeforeOrderByUpdatedAtAsc(
                PostImageUploadStatus.PENDING,
                threshold
        );
    }

    @Transactional(readOnly = true)
    public List<PostImageUpload> readStaleCompensating(LocalDateTime threshold) {
        return repository.findTop100ByStatusAndUpdatedAtBeforeOrderByUpdatedAtAsc(
                PostImageUploadStatus.COMPENSATING,
                threshold
        );
    }

    @Transactional(readOnly = true)
    public List<PostImageUpload> readCompletedWithStaging() {
        return repository.findTop100ByStatusAndStagingCleanedFalseOrderByUpdatedAtAsc(
                PostImageUploadStatus.COMPLETED
        );
    }

    @Transactional(readOnly = true)
    public List<PostImageUpload> readUnclaimedCompleted(LocalDateTime threshold) {
        return repository.findTop100ByStatusAndClaimedPostIdIsNullAndUpdatedAtBeforeOrderByUpdatedAtAsc(
                PostImageUploadStatus.COMPLETED,
                threshold
        );
    }

    @Transactional
    public boolean claimUnclaimedCompleted(Long uploadId, LocalDateTime threshold) {
        return repository.claimUnclaimedCompleted(
                uploadId,
                threshold,
                PostImageUploadStatus.COMPLETED,
                PostImageUploadStatus.COMPENSATING
        ) == 1;
    }

    private PostImageUpload readById(Long uploadId) {
        return repository.findById(uploadId)
                .orElseThrow(() -> new IllegalStateException("이미지 업로드 작업을 찾을 수 없습니다."));
    }
}
