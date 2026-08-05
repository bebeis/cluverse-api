package cluverse.post.domain;

import cluverse.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostImageAsset extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_image_asset_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_image_upload_id", nullable = false)
    private PostImageUpload upload;

    @Column(nullable = false)
    private int displayOrder;

    private String stagingKey;

    @Column(nullable = false)
    private String contentKey;

    private String thumbnailKey;

    @Column(nullable = false)
    private long sourceBytes;

    private String contentType;
    private Integer contentWidth;
    private Integer contentHeight;
    private Long contentBytes;
    private Integer thumbnailWidth;
    private Integer thumbnailHeight;
    private Long thumbnailBytes;

    private PostImageAsset(
            int displayOrder,
            String stagingKey,
            String contentKey,
            String thumbnailKey,
            long sourceBytes
    ) {
        this.displayOrder = displayOrder;
        this.stagingKey = stagingKey;
        this.contentKey = contentKey;
        this.thumbnailKey = thumbnailKey;
        this.sourceBytes = sourceBytes;
    }

    public static PostImageAsset plan(
            int displayOrder,
            String stagingKey,
            String contentKey,
            String thumbnailKey,
            long sourceBytes
    ) {
        return new PostImageAsset(displayOrder, stagingKey, contentKey, thumbnailKey, sourceBytes);
    }

    public static PostImageAsset completedOriginal(
            int displayOrder,
            String contentKey,
            String contentType,
            long bytes
    ) {
        PostImageAsset asset = new PostImageAsset(displayOrder, null, contentKey, null, bytes);
        asset.contentType = contentType;
        asset.contentBytes = bytes;
        return asset;
    }

    void attach(PostImageUpload upload) {
        this.upload = upload;
    }

    void complete(ProcessedPostImage processed) {
        if (displayOrder != processed.displayOrder()) {
            throw new IllegalArgumentException("이미지 순서가 일치하지 않습니다.");
        }
        PostImageMetadata content = processed.content();
        validateObjectKey(contentKey, content.objectKey());
        this.contentType = content.contentType();
        this.contentWidth = content.width();
        this.contentHeight = content.height();
        this.contentBytes = content.bytes();

        if (thumbnailKey != null) {
            PostImageMetadata thumbnail = processed.thumbnail();
            if (thumbnail == null) {
                throw new IllegalArgumentException("썸네일 결과가 없습니다.");
            }
            validateObjectKey(thumbnailKey, thumbnail.objectKey());
            this.thumbnailWidth = thumbnail.width();
            this.thumbnailHeight = thumbnail.height();
            this.thumbnailBytes = thumbnail.bytes();
        }
    }

    long outputBytes() {
        long contentSize = contentBytes == null ? 0 : contentBytes;
        long thumbnailSize = thumbnailBytes == null ? 0 : thumbnailBytes;
        return contentSize + thumbnailSize;
    }

    private void validateObjectKey(String expected, String actual) {
        if (!expected.equals(actual)) {
            throw new IllegalArgumentException("예정된 object key와 처리 결과가 일치하지 않습니다.");
        }
    }
}
