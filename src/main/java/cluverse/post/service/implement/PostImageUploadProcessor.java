package cluverse.post.service.implement;

import cluverse.post.domain.ProcessedPostImage;

import java.util.List;

public interface PostImageUploadProcessor {

    List<ProcessedPostImage> process(List<PreparedPostImage> images);
}
