package cluverse.post.domain;

public record PostImageMetadata(
        String objectKey,
        String contentType,
        int width,
        int height,
        long bytes
) {
}
