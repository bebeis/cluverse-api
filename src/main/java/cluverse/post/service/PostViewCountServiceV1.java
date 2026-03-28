package cluverse.post.service;

import cluverse.meta.service.implement.PostMetaWriter;
import cluverse.post.service.implement.PostAccessReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class PostViewCountServiceV1 {

    private final PostAccessReader postAccessReader;
    private final PostMetaWriter postMetaWriter;

    public void increaseViewCountV1(Long postId) {
        postAccessReader.readOrThrow(postId);
        postMetaWriter.increaseViewCount(postId);
    }

    public void increaseViewCountV2(Long postId) {
        postAccessReader.readOrThrow(postId);
        postMetaWriter.increaseViewCountV2(postId);
    }
}
