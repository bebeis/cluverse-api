package cluverse.post.domain;

import cluverse.common.entity.BaseTimeEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "post_image_upload",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_post_image_upload_version_request",
                columnNames = {"version", "request_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostImageUpload extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_image_upload_id")
    private Long id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "request_id", nullable = false, length = 36)
    private UUID requestId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    private ImageUploadVersion version;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PostImageUploadStatus status;

    @OneToMany(mappedBy = "upload", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    private List<PostImageAsset> assets = new ArrayList<>();

    @Column(length = 500)
    private String failureReason;

    @Column(nullable = false)
    private boolean stagingCleaned;

    private PostImageUpload(
            UUID requestId,
            ImageUploadVersion version,
            PostImageUploadStatus status,
            List<PostImageAsset> assets,
            boolean stagingCleaned
    ) {
        this.requestId = requestId;
        this.version = version;
        this.status = status;
        this.stagingCleaned = stagingCleaned;
        replaceAssets(assets);
    }

    public static PostImageUpload reserve(
            UUID requestId,
            ImageUploadVersion version,
            List<PostImageAsset> assets
    ) {
        return new PostImageUpload(requestId, version, PostImageUploadStatus.PENDING, assets, false);
    }

    public static PostImageUpload completed(
            UUID requestId,
            ImageUploadVersion version,
            List<PostImageAsset> assets
    ) {
        return new PostImageUpload(requestId, version, PostImageUploadStatus.COMPLETED, assets, true);
    }

    public void complete(List<ProcessedPostImage> processedImages) {
        validatePending();
        List<ProcessedPostImage> orderedResults = processedImages.stream()
                .sorted(Comparator.comparingInt(ProcessedPostImage::displayOrder))
                .toList();
        if (assets.size() != orderedResults.size()) {
            throw new IllegalArgumentException("예약한 이미지 수와 처리 결과 수가 일치하지 않습니다.");
        }
        for (int index = 0; index < assets.size(); index++) {
            assets.get(index).complete(orderedResults.get(index));
        }
        status = PostImageUploadStatus.COMPLETED;
    }

    public void fail(String reason) {
        validatePending();
        status = PostImageUploadStatus.FAILED;
        failureReason = truncateFailureReason(reason);
    }

    public void completeCompensation(String reason) {
        if (status != PostImageUploadStatus.COMPENSATING) {
            throw new IllegalStateException("COMPENSATING 업로드만 보상을 완료할 수 있습니다.");
        }
        status = PostImageUploadStatus.FAILED;
        failureReason = truncateFailureReason(reason);
    }

    private String truncateFailureReason(String reason) {
        return reason == null ? "unknown" : reason.substring(0, Math.min(reason.length(), 500));
    }

    public void markStagingCleaned() {
        stagingCleaned = true;
    }

    public long getTotalSourceBytes() {
        return assets.stream().mapToLong(PostImageAsset::getSourceBytes).sum();
    }

    public long getTotalOutputBytes() {
        return assets.stream().mapToLong(PostImageAsset::outputBytes).sum();
    }

    private void replaceAssets(List<PostImageAsset> newAssets) {
        assets.clear();
        newAssets.stream()
                .sorted(Comparator.comparingInt(PostImageAsset::getDisplayOrder))
                .forEach(asset -> {
                    asset.attach(this);
                    assets.add(asset);
                });
    }

    private void validatePending() {
        if (status != PostImageUploadStatus.PENDING) {
            throw new IllegalStateException("PENDING 업로드만 상태를 변경할 수 있습니다.");
        }
    }
}
