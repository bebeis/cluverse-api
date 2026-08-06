package cluverse.post.service.response;

import cluverse.post.domain.UploadedPostImage;

public record UploadedPostImageResponse(
        String fileKey,
        String imageUrl
) {
    public static UploadedPostImageResponse from(UploadedPostImage image) {
        return new UploadedPostImageResponse(image.fileKey(), image.imageUrl());
    }
}
