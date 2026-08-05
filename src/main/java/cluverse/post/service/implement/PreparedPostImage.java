package cluverse.post.service.implement;

import cluverse.post.client.PostImageProcessCommand;

import java.nio.file.Path;

public record PreparedPostImage(
        Path path,
        String contentType,
        long sourceBytes,
        PostImageProcessCommand command
) {
}
