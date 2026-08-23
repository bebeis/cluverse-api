package cluverse.post.service;

import cluverse.meta.service.implement.TotalViewCountCounter;
import cluverse.post.service.implement.PostAccessReader;
import cluverse.post.service.response.PostViewCountResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PostViewCountService {

    private final PostAccessReader postAccessReader;
    private final TotalViewCountCounter totalViewCountCounter;

    public PostViewCountResponse increaseViewCount(Long postId, String cookieId) {
        postAccessReader.validateActivePost(postId);
        return PostViewCountResponse.of(postId, totalViewCountCounter.count(postId, cookieId));
    }
}
