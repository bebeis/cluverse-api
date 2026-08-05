package cluverse.post.domain;

public record ProcessedPostImage(
        int displayOrder,
        PostImageMetadata content,
        PostImageMetadata thumbnail
) {
}
