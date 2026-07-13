package cluverse.meta.service.implement;

import cluverse.common.exception.BadRequestException;
import cluverse.meta.domain.PostViewCount;
import cluverse.meta.domain.PostViewCountOptimistic;
import cluverse.meta.exception.MetaExceptionMessage;
import cluverse.meta.repository.PostBookmarkCountRepository;
import cluverse.meta.repository.PostCommentCountRepository;
import cluverse.meta.repository.PostLikeCountRepository;
import cluverse.meta.repository.PostViewCountRepository;
import cluverse.meta.repository.PostViewCountOptimisticRepository;
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
    private final PostViewCountOptimisticRepository postViewCountOptimisticRepository;
    private final TransactionTemplate requiresNewTransactionTemplate;

    public PostMetaWriter(
            PostLikeCountRepository postLikeCountRepository,
            PostBookmarkCountRepository postBookmarkCountRepository,
            PostCommentCountRepository postCommentCountRepository,
            PostViewCountRepository postViewCountRepository,
            PostViewCountOptimisticRepository postViewCountOptimisticRepository,
            PlatformTransactionManager transactionManager
    ) {
        this.postLikeCountRepository = postLikeCountRepository;
        this.postBookmarkCountRepository = postBookmarkCountRepository;
        this.postCommentCountRepository = postCommentCountRepository;
        this.postViewCountRepository = postViewCountRepository;
        this.postViewCountOptimisticRepository = postViewCountOptimisticRepository;
        this.requiresNewTransactionTemplate = new TransactionTemplate(transactionManager);
        this.requiresNewTransactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public void createViewCount(Long postId) {
        postViewCountRepository.save(PostViewCount.of(postId, 0));
    }

    /**
     * [V3] 원자적 UPDATE 조회수 증가 — 운영 방식.
     */
    public void increaseViewCount(Long postId) {
        postViewCountRepository.increaseCount(postId);
    }

    /**
     * [V2] 비관적 락(select for update) 조회수 증가.
     * 락 획득부터 트랜잭션 커밋까지 레코드 락을 보유한다. UPDATE는 더티체킹으로 커밋 시점에 발행된다.
     */
    public void increaseViewCountPessimistic(Long postId) {
        postViewCountRepository.findByPostIdForUpdate(postId)
                .orElseThrow(() -> new BadRequestException(MetaExceptionMessage.POST_VIEW_COUNT_NOT_FOUND.getMessage()))
                .increase();
    }

    /**
     * [V1] 낙관적 락(@Version) 조회수 증가. 버전 충돌 시 새 트랜잭션으로 재시도한다.
     */
    public void increaseViewCountOptimistic(Long postId) {
        for (int attempt = 0; attempt < MAX_RETRY_COUNT; attempt++) {
            try {
                requiresNewTransactionTemplate.executeWithoutResult(status -> increaseViewCountOptimisticInternal(postId));
                return;
            } catch (ObjectOptimisticLockingFailureException
                     | OptimisticLockException
                     | DataIntegrityViolationException exception) {
                if (attempt == MAX_RETRY_COUNT - 1) {
                    throw new IllegalStateException(
                            MetaExceptionMessage.POST_VIEW_COUNT_OPTIMISTIC_INCREASE_FAILED.getMessage(),
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

    private void increaseViewCountOptimisticInternal(Long postId) {
        PostViewCountOptimistic postViewCount = postViewCountOptimisticRepository.findById(postId)
                .orElseGet(() -> postViewCountOptimisticRepository.save(PostViewCountOptimistic.create(postId)));
        postViewCount.increase();
        postViewCountOptimisticRepository.flush();
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
