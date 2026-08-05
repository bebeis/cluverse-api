package cluverse.post.service.response;

import cluverse.post.domain.PostImageAsset;

public record PostImageAssetResponse(
        int displayOrder,
        String contentKey,
        String thumbnailKey,
        long sourceBytes,
        Long contentBytes,
        Long thumbnailBytes
) {
    public static PostImageAssetResponse of(PostImageAsset asset) {
        return new PostImageAssetResponse(
                asset.getDisplayOrder(),
                asset.getContentKey(),
                asset.getThumbnailKey(),
                asset.getSourceBytes(),
                asset.getContentBytes(),
                asset.getThumbnailBytes()
        );
    }
}
