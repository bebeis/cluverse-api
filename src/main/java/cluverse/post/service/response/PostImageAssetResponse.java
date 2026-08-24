package cluverse.post.service.response;

import cluverse.post.domain.PostImageAsset;

import java.util.function.Function;

public record PostImageAssetResponse(
        int displayOrder,
        String contentKey,
        String contentUrl,
        String thumbnailKey,
        String thumbnailUrl,
        long sourceBytes,
        Long contentBytes,
        Long thumbnailBytes
) {
    public static PostImageAssetResponse of(PostImageAsset asset, Function<String, String> imageUrlResolver) {
        return new PostImageAssetResponse(
                asset.getDisplayOrder(),
                asset.getContentKey(),
                resolveUrl(asset.getContentKey(), imageUrlResolver),
                asset.getThumbnailKey(),
                resolveUrl(asset.getThumbnailKey(), imageUrlResolver),
                asset.getSourceBytes(),
                asset.getContentBytes(),
                asset.getThumbnailBytes()
        );
    }

    private static String resolveUrl(String objectKey, Function<String, String> imageUrlResolver) {
        return objectKey == null ? null : imageUrlResolver.apply(objectKey);
    }
}
