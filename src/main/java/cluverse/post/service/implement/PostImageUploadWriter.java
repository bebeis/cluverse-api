package cluverse.post.service.implement;

import cluverse.post.domain.ImageUploadVersion;
import cluverse.post.domain.PostImageAsset;
import cluverse.post.domain.PostImageUpload;
import cluverse.post.domain.PostImageUploadStatus;
import cluverse.post.domain.ProcessedPostImage;
import cluverse.post.repository.PostImageUploadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PostImageUploadWriter {

    private final PostImageUploadRepository repository;

    @Transactional
    public PostImageUpload saveCompleted(PostImageUpload upload) {
        return repository.save(upload);
    }

    @Transactional
    public PostImageUpload reserve(
            UUID requestId,
            ImageUploadVersion version,
            List<PostImageAsset> assets
    ) {
        return repository.saveAndFlush(PostImageUpload.reserve(requestId, version, assets));
    }

    @Transactional(readOnly = true)
    public Optional<PostImageUpload> read(UUID requestId, ImageUploadVersion version) {
        return repository.findByRequestIdAndVersion(requestId, version);
    }

    @Transactional
    public PostImageUpload complete(Long uploadId, List<ProcessedPostImage> processedImages) {
        PostImageUpload upload = readById(uploadId);
        upload.complete(processedImages);
        return upload;
    }

    @Transactional
    public void fail(Long uploadId, String reason) {
        PostImageUpload upload = readById(uploadId);
        if (upload.getStatus() == PostImageUploadStatus.PENDING) {
            upload.fail(reason);
        }
    }

    @Transactional
    public void markStagingCleaned(Long uploadId) {
        readById(uploadId).markStagingCleaned();
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
    public List<PostImageUpload> readCompletedWithStaging() {
        return repository.findTop100ByStatusAndStagingCleanedFalseOrderByUpdatedAtAsc(
                PostImageUploadStatus.COMPLETED
        );
    }

    @Transactional(readOnly = true)
    public long countPending() {
        return repository.countByStatus(PostImageUploadStatus.PENDING);
    }

    private PostImageUpload readById(Long uploadId) {
        return repository.findById(uploadId)
                .orElseThrow(() -> new IllegalStateException("이미지 업로드 작업을 찾을 수 없습니다."));
    }
}
