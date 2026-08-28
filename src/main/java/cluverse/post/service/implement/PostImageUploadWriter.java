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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PostImageUploadWriter {

    private final PostImageUploadRepository repository;

    @Transactional
    public PostImageUpload reserve(
            Long memberId,
            UUID requestId,
            ImageUploadVersion version,
            List<PostImageAsset> assets
    ) {
        return repository.saveAndFlush(PostImageUpload.reserve(memberId, requestId, version, assets));
    }

    @Transactional
    public PostImageUpload reserve(
            UUID requestId,
            ImageUploadVersion version,
            List<PostImageAsset> assets
    ) {
        return reserve(null, requestId, version, assets);
    }

    @Transactional(readOnly = true)
    public Optional<PostImageUpload> read(UUID requestId, ImageUploadVersion version) {
        return repository.findByRequestIdAndVersion(requestId, version);
    }

    @Transactional
    public PostImageUpload complete(Long uploadId, List<ProcessedPostImage> processedImages) {
        // stale 재조정의 PENDING 점유와 정상 완료가 동시에 상태를 바꾸지 못하게 행을 잠근다.
        PostImageUpload upload = repository.findByIdForUpdate(uploadId)
                .orElseThrow(() -> new IllegalStateException("이미지 업로드 작업을 찾을 수 없습니다."));
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

    private PostImageUpload readById(Long uploadId) {
        return repository.findById(uploadId)
                .orElseThrow(() -> new IllegalStateException("이미지 업로드 작업을 찾을 수 없습니다."));
    }
}
