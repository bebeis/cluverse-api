package cluverse.post.service;

import cluverse.meta.service.PostMetaService;
import cluverse.post.service.implement.PostReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class PostViewCountServiceV1 {

    private final PostReader postReader;
    private final PostMetaService postMetaService;

    public void increaseViewCountV1(Long postId) {
        postReader.readOrThrow(postId);
        postMetaService.increaseViewCount(postId);
    }

    public void increaseViewCountV2(Long postId) {
        postReader.readOrThrow(postId);
        postMetaService.increaseViewCountV2(postId);
    }
}
