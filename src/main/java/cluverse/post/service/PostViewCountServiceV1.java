package cluverse.post.service;

import cluverse.meta.service.implement.PostMetaWriter;
import cluverse.post.service.implement.PostAccessReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * [V1] 낙관적 락(@Version + 재시도) 조회수 증가.
 * 충돌 시 새 트랜잭션으로 재시도하며, 재시도 소진 시 실패한다.
 * 의도적으로 트랜잭션을 선언하지 않는다 — 외부 트랜잭션이 있으면 커넥션을 쥔 채
 * 내부 REQUIRES_NEW가 두 번째 커넥션을 기다려 풀 포화 시 데드락이 발생한다.
 */
@Service
@RequiredArgsConstructor
public class PostViewCountServiceV1 {

    private final PostAccessReader postAccessReader;
    private final PostMetaWriter postMetaWriter;

    public void increaseViewCount(Long postId) {
        postAccessReader.readOrThrow(postId);
        postMetaWriter.increaseViewCountOptimistic(postId);
    }
}
