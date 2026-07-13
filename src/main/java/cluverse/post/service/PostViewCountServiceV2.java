package cluverse.post.service;

import cluverse.meta.service.implement.PostMetaWriter;
import cluverse.post.service.implement.PostAccessReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * [V2] 비관적 락(select for update + 더티체킹) 조회수 증가.
 * 락 획득부터 커밋까지가 락 보유 구간이며, 이 구간의 길이가 측정 대상이다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class PostViewCountServiceV2 {

    private final PostAccessReader postAccessReader;
    private final PostMetaWriter postMetaWriter;

    public void increaseViewCount(Long postId) {
        postAccessReader.readOrThrow(postId);
        postMetaWriter.increaseViewCountPessimistic(postId);
    }
}
