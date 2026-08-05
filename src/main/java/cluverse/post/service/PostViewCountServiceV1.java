package cluverse.post.service;

import cluverse.meta.service.implement.PostMetaReader;
import cluverse.meta.service.implement.PostMetaWriter;
import cluverse.meta.service.implement.ViewCountResult;
import cluverse.meta.service.implement.ViewCountSource;
import cluverse.post.service.implement.PostAccessReader;
import cluverse.post.service.response.PostViewCountResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PostViewCountServiceV1 {

    private final PostAccessReader postAccessReader;
    private final PostMetaWriter postMetaWriter;
    private final PostMetaReader postMetaReader;

    public PostViewCountResponse increaseViewCount(Long postId) {
        postAccessReader.validateActivePost(postId);
        postMetaWriter.increaseViewCount(postId);
        return PostViewCountResponse.of(
                postId,
                new ViewCountResult(postMetaReader.readViewCount(postId), true, ViewCountSource.MYSQL)
        );
    }
}
