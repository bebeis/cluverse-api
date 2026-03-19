package cluverse.meta.service.implement;

import cluverse.meta.domain.PostViewCountV2;
import cluverse.meta.repository.PostViewCountV2Repository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class PostViewCountV2TransactionWriter {

    private final PostViewCountV2Repository postViewCountV2Repository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void increaseCount(Long postId) {
        PostViewCountV2 postViewCount = postViewCountV2Repository.findById(postId)
                .orElseGet(() -> postViewCountV2Repository.save(PostViewCountV2.create(postId)));
        postViewCount.increase();
        postViewCountV2Repository.flush();
    }
}
