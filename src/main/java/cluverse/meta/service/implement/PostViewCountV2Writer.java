package cluverse.meta.service.implement;

import cluverse.meta.domain.PostViewCountV2;
import cluverse.meta.exception.MetaExceptionMessage;
import cluverse.meta.repository.PostViewCountV2Repository;
import jakarta.persistence.OptimisticLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class PostViewCountV2Writer {

    private static final int MAX_RETRY_COUNT = 10;

    private final PostViewCountV2Repository postViewCountV2Repository;
    private final TransactionTemplate transactionTemplate;

    public PostViewCountV2Writer(PostViewCountV2Repository postViewCountV2Repository,
                                 PlatformTransactionManager transactionManager) {
        this.postViewCountV2Repository = postViewCountV2Repository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public void increaseCount(Long postId) {
        for (int retryCount = 0; retryCount < MAX_RETRY_COUNT; retryCount++) {
            try {
                transactionTemplate.executeWithoutResult(status -> doIncreaseCount(postId));
                return;
            } catch (ObjectOptimisticLockingFailureException
                     | OptimisticLockException
                     | DataIntegrityViolationException exception) {
                if (isLastRetry(retryCount)) {
                    throw new IllegalStateException(
                            MetaExceptionMessage.POST_VIEW_COUNT_V2_INCREASE_FAILED.getMessage(),
                            exception
                    );
                }
            }
        }
    }

    private void doIncreaseCount(Long postId) {
        PostViewCountV2 postViewCount = postViewCountV2Repository.findById(postId)
                .orElseGet(() -> postViewCountV2Repository.save(PostViewCountV2.create(postId)));
        postViewCount.increase();
        postViewCountV2Repository.flush();
    }

    private boolean isLastRetry(int retryCount) {
        return retryCount == MAX_RETRY_COUNT - 1;
    }
}
