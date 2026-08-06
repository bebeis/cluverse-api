package cluverse.post.service.response;

import cluverse.post.domain.UploadedPostImage;

import java.util.List;

public record PostImageMultipartUploadResponse(
        List<UploadedPostImageResponse> images
) {
    public PostImageMultipartUploadResponse {
        images = List.copyOf(images);
    }

    public static PostImageMultipartUploadResponse from(List<UploadedPostImage> images) {
        return new PostImageMultipartUploadResponse(
                images.stream().map(UploadedPostImageResponse::from).toList()
        );
    }
}
