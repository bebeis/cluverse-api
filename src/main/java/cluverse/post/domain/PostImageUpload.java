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

    private Long memberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    private ImageUploadVersion version;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PostImageUploadStatus status;

    private Long claimedPostId;

    private java.time.LocalDateTime claimedAt;

    @OneToMany(mappedBy = "upload", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    private List<PostImageAsset> assets = new ArrayList<>();

    @Column(length = 500)
    private String failureReason;

    @Column(nullable = false)
    private boolean stagingCleaned;

    private PostImageUpload(
            UUID requestId,
            Long memberId,
            ImageUploadVersion version,
            PostImageUploadStatus status,
            List<PostImageAsset> assets,
            boolean stagingCleaned
    ) {
        this.requestId = requestId;
        this.memberId = memberId;
        this.version = version;
        this.status = status;
        this.stagingCleaned = stagingCleaned;
        replaceAssets(assets);
    }

    public static PostImageUpload reserve(
            Long memberId,
            UUID requestId,
            ImageUploadVersion version,
            List<PostImageAsset> assets
    ) {
        return new PostImageUpload(requestId, memberId, version, PostImageUploadStatus.PENDING, assets, false);
    }

    public static PostImageUpload reserve(
            UUID requestId,
            ImageUploadVersion version,
            List<PostImageAsset> assets
    ) {
        return reserve(null, requestId, version, assets);
    }

    public static PostImageUpload completed(
            UUID requestId,
            ImageUploadVersion version,
            List<PostImageAsset> assets
    ) {
        return completed(null, requestId, version, assets);
    }

    public static PostImageUpload completed(
            Long memberId,
            UUID requestId,
            ImageUploadVersion version,
            List<PostImageAsset> assets
    ) {
        return new PostImageUpload(
                requestId, memberId, version, PostImageUploadStatus.COMPLETED, assets, true);
    }

    public void validateOwner(Long requestedMemberId) {
        if (memberId != null && !memberId.equals(requestedMemberId)) {
            throw new IllegalArgumentException("이미지 업로드 소유자가 일치하지 않습니다.");
        }
    }

    public void claim(Long requestedMemberId, Long postId) {
        validateOwner(requestedMemberId);
        if (status != PostImageUploadStatus.COMPLETED) {
            throw new IllegalStateException("완료된 이미지 업로드만 게시글에 연결할 수 있습니다.");
        }
        if (claimedPostId != null && !claimedPostId.equals(postId)) {
            throw new IllegalStateException("이미 다른 게시글에 연결된 이미지 업로드입니다.");
        }
        claimedPostId = postId;
        claimedAt = java.time.LocalDateTime.now();
    }

    public void release(Long postId) {
        if (claimedPostId != null && claimedPostId.equals(postId)) {
            claimedPostId = null;
            claimedAt = null;
        }
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
