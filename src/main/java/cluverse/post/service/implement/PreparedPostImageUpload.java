package cluverse.post.service.implement;

import cluverse.post.domain.PostImageAsset;

import java.util.List;

public record PreparedPostImageUpload(
        List<PreparedPostImage> images,
        List<PostImageAsset> assets,
        PostImageUploadTemporaryFileCleaner temporaryFileCleaner
) implements AutoCloseable {

    @Override
    public void close() {
        for (PreparedPostImage image : images) {
            temporaryFileCleaner.delete(image.path());
        }
    }
}
