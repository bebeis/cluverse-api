package cluverse.post.service.implement;

import cluverse.post.domain.ImageUploadVersion;
import cluverse.post.domain.ProcessedPostImage;
import cluverse.post.service.request.ImageUploadFailurePoint;

import java.util.List;

public interface PostImageUploadProcessor {

    ImageUploadVersion version();

    List<ProcessedPostImage> process(
            List<PreparedPostImage> images,
            ImageUploadFailurePoint failurePoint
    );
}
