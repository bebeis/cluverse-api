package cluverse.post.service.implement;

import cluverse.post.domain.PostImageAsset;

import java.util.List;

public record PreparedPostImageUpload(
        List<PreparedPostImage> images,
        List<PostImageAsset> assets
) implements AutoCloseable {

    @Override
    public void close() {
        for (PreparedPostImage image : images) {
            image.source().close();
        }
    }
}
