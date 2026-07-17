package cluverse.post.service;

import cluverse.meta.service.implement.PostMetaWriter;
import cluverse.post.service.implement.PostAccessReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * [V3] 원자적 UPDATE 조회수 증가.
 * 운영 구현({@link PostMetaWriter#increaseViewCount})에 그대로 위임한다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class PostViewCountServiceV3 {

    private final PostAccessReader postAccessReader;
    private final PostMetaWriter postMetaWriter;

    public void increaseViewCount(Long postId) {
        postAccessReader.readOrThrow(postId);
        postMetaWriter.increaseViewCount(postId);
    }
}
