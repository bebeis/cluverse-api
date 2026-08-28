package cluverse.post.service.implement;

import cluverse.post.domain.PostImageProcessingPlan;

public record PreparedPostImage(
        TemporaryPostImageFile source,
        String contentType,
        long sourceBytes,
        PostImageProcessingPlan plan
) {
    public java.nio.file.Path path() {
        return source.path();
    }
}
