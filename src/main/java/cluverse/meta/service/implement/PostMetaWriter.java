package cluverse.meta.service.implement;

import cluverse.common.exception.BadRequestException;
import cluverse.meta.domain.PostViewCount;
import cluverse.meta.domain.PostViewCountV2;
import cluverse.meta.exception.MetaExceptionMessage;
import cluverse.meta.repository.PostBookmarkCountRepository;
import cluverse.meta.repository.PostCommentCountRepository;
import cluverse.meta.repository.PostLikeCountRepository;
import cluverse.meta.repository.PostViewCountRepository;
import cluverse.meta.repository.PostViewCountV2Repository;
import jakarta.persistence.OptimisticLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Component
@Transactional
public class PostMetaWriter {

    private static final int MAX_RETRY_COUNT = 10;
    private static final long RETRY_DELAY_MILLIS = 10L;

    private final PostLikeCountRepository postLikeCountRepository;
    private final PostBookmarkCountRepository postBookmarkCountRepository;
    private final PostCommentCountRepository postCommentCountRepository;
    private final PostViewCountRepository postViewCountRepository;
    private final PostViewCountV2Repository postViewCountV2Repository;
    private final TransactionTemplate requiresNewTransactionTemplate;

    public PostMetaWriter(
            PostLikeCountRepository postLikeCountRepository,
            PostBookmarkCountRepository postBookmarkCountRepository,
            PostCommentCountRepository postCommentCountRepository,
            PostViewCountRepository postViewCountRepository,
            PostViewCountV2Repository postViewCountV2Repository,
            PlatformTransactionManager transactionManager
    ) {
        this.postLikeCountRepository = postLikeCountRepository;
        this.postBookmarkCountRepository = postBookmarkCountRepository;
        this.postCommentCountRepository = postCommentCountRepository;
        this.postViewCountRepository = postViewCountRepository;
        this.postViewCountV2Repository = postViewCountV2Repository;
        this.requiresNewTransactionTemplate = new TransactionTemplate(transactionManager);
        this.requiresNewTransactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public void createViewCount(Long postId) {
        postViewCountRepository.save(PostViewCount.of(postId, 0));
    }

    public void increaseViewCount(Long postId) {
        postViewCountRepository.increaseCount(postId);
    }

    public void increaseViewCountV2(Long postId) {
        for (int attempt = 0; attempt < MAX_RETRY_COUNT; attempt++) {
            try {
                requiresNewTransactionTemplate.executeWithoutResult(status -> increaseViewCountV2Internal(postId));
                return;
            } catch (ObjectOptimisticLockingFailureException
                     | OptimisticLockException
                     | DataIntegrityViolationException exception) {
                if (attempt == MAX_RETRY_COUNT - 1) {
                    throw new IllegalStateException(
                            MetaExceptionMessage.POST_VIEW_COUNT_V2_INCREASE_FAILED.getMessage(),
                            exception
                    );
                }
                pauseBeforeRetry();
            }
        }
    }

    public void increaseLikeCount(Long postId) {
        postLikeCountRepository.increaseCount(postId);
    }

    public void increaseBookmarkCount(Long postId) {
        postBookmarkCountRepository.increaseCount(postId);
    }

    public void decreaseBookmarkCount(Long postId) {
        postBookmarkCountRepository.findByPostIdForUpdate(postId)
                .orElseThrow(() -> new BadRequestException(MetaExceptionMessage.POST_BOOKMARK_COUNT_ALREADY_ZERO.getMessage()));
        int updatedRowCount = postBookmarkCountRepository.decreaseCount(postId);
        validateUpdated(updatedRowCount, MetaExceptionMessage.POST_BOOKMARK_COUNT_ALREADY_ZERO);
        postBookmarkCountRepository.deleteIfZero(postId);
    }

    public void increaseCommentCount(Long postId) {
        postCommentCountRepository.increaseCount(postId);
    }

    public void decreaseCommentCount(Long postId) {
        postCommentCountRepository.findByPostIdForUpdate(postId)
                .orElseThrow(() -> new BadRequestException(MetaExceptionMessage.POST_COMMENT_COUNT_ALREADY_ZERO.getMessage()));
        int updatedRowCount = postCommentCountRepository.decreaseCount(postId);
        validateUpdated(updatedRowCount, MetaExceptionMessage.POST_COMMENT_COUNT_ALREADY_ZERO);
        postCommentCountRepository.deleteIfZero(postId);
    }

    private void validateUpdated(int updatedRowCount, MetaExceptionMessage message) {
        if (updatedRowCount == 0) {
            throw new BadRequestException(message.getMessage());
        }
    }

    private void increaseViewCountV2Internal(Long postId) {
        PostViewCountV2 postViewCount = postViewCountV2Repository.findById(postId)
                .orElseGet(() -> postViewCountV2Repository.save(PostViewCountV2.create(postId)));
        postViewCount.increase();
        postViewCountV2Repository.flush();
    }

    private void pauseBeforeRetry() {
        try {
            Thread.sleep(RETRY_DELAY_MILLIS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Retry interrupted.", exception);
        }
    }
}
