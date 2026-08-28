package cluverse.post.service.implement;

import cluverse.post.repository.LatestPostIdCacheRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
public class PostListCacheInvalidator {

    private final LatestPostIdCacheRepository cacheRepository;
    private final Counter invalidationSuccess;
    private final Counter invalidationError;

    public PostListCacheInvalidator(
            LatestPostIdCacheRepository cacheRepository,
            MeterRegistry meterRegistry
    ) {
        this.cacheRepository = cacheRepository;
        this.invalidationSuccess = invalidationCounter(meterRegistry, "success");
        this.invalidationError = invalidationCounter(meterRegistry, "error");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void invalidate(PostListChangedEvent event) {
        try {
            // 롤백된 쓰기는 캐시를 비우지 않는다. 커밋된 변경만 버전을 올려 진행 중인 워밍도 무효화한다.
            cacheRepository.invalidateBoard(event.boardId());
            invalidationSuccess.increment();
        } catch (RuntimeException exception) {
            invalidationError.increment();
            log.warn("커밋된 게시글 변경 후 목록 캐시 무효화에 실패했습니다. TTL 만료로 복구합니다. boardId={}",
                    event.boardId(), exception);
        }
    }

    private Counter invalidationCounter(MeterRegistry meterRegistry, String result) {
        return Counter.builder("post.list.cache.invalidations")
                .description("게시글 쓰기 이후 최신순 ID 캐시 무효화")
                .tag("result", result)
                .register(meterRegistry);
    }
}
