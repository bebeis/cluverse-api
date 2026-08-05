package cluverse.post.service;

import cluverse.meta.service.implement.DeltaViewCountCounter;
import cluverse.post.service.implement.PostAccessReader;
import cluverse.post.service.response.PostViewCountResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PostViewCountServiceV2 {

    private final PostAccessReader postAccessReader;
    private final DeltaViewCountCounter deltaViewCountCounter;

    public PostViewCountResponse increaseViewCount(Long postId, String cookieId) {
        postAccessReader.validateActivePost(postId);
        return PostViewCountResponse.of(postId, deltaViewCountCounter.countTimeBased(postId, cookieId));
    }
}
