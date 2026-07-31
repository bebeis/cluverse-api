package cluverse.post.service;

import cluverse.meta.service.implement.PostMetaWriter;
import cluverse.meta.service.implement.ViewCountBufferWriter;
import cluverse.meta.service.implement.ViewSurgeDetector;
import cluverse.meta.service.implement.ViewSurgeRoutingCache;
import cluverse.post.service.implement.PostAccessReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * [V4] 급상승 감지 + Redis Write-back 조회수 증가.
 * 의도적으로 트랜잭션을 선언하지 않는다 — Redis 분기가 DB 커넥션을 쥐면 안 된다.
 */
@Service
@RequiredArgsConstructor
public class PostViewCountServiceV4 {

    private final PostAccessReader postAccessReader;
    private final ViewSurgeRoutingCache viewSurgeRoutingCache;
    private final ViewCountBufferWriter viewCountBufferWriter;
    private final PostMetaWriter postMetaWriter;
    private final ViewSurgeDetector viewSurgeDetector;

    public void increaseViewCount(Long postId) {
        postAccessReader.validateActivePost(postId);

        if (viewSurgeRoutingCache.contains(postId) && viewCountBufferWriter.tryIncrease(postId)) {
            return;
        }

        long newCount = postMetaWriter.increaseViewCountAndGet(postId);
        viewSurgeDetector.observe(postId, newCount);
    }
}
