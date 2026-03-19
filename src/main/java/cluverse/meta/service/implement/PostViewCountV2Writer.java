package cluverse.meta.service.implement;

import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PostViewCountV2Writer {

    private static final int MAX_RETRY_COUNT = 10;
    private static final long RETRY_DELAY_MILLIS = 10L;

    private final PostViewCountV2TransactionWriter postViewCountV2TransactionWriter;

    @Retryable(
            includes = {
                    ObjectOptimisticLockingFailureException.class,
                    OptimisticLockException.class,
                    DataIntegrityViolationException.class
            },
            maxRetries = MAX_RETRY_COUNT - 1,
            delay = RETRY_DELAY_MILLIS
    )
    public void increaseCount(Long postId) {
        postViewCountV2TransactionWriter.increaseCount(postId);
    }
}
