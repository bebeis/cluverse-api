package cluverse.post.client;

import cluverse.post.domain.ProcessedPostImage;

public interface PostImageProcessorClient {

    ProcessedPostImage process(PostImageProcessCommand command);
}
