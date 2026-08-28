package cluverse.post.service.implement;

import cluverse.post.domain.PostCategory;
import cluverse.post.properties.PostListCacheProperties;
import cluverse.post.repository.LatestPostIdCacheRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
public class LatestPostIdCacheWriter {

    private final LatestPostIdCacheRepository cacheRepository;
    private final PostListCacheProperties properties;
    private final Counter updated;
    private final Counter skipped;
    private final Counter error;

    public LatestPostIdCacheWriter(
            LatestPostIdCacheRepository cacheRepository,
            PostListCacheProperties properties,
            MeterRegistry meterRegistry
    ) {
        this.cacheRepository = cacheRepository;
        this.properties = properties;
        this.updated = writeThroughCounter(meterRegistry, "updated");
        this.skipped = writeThroughCounter(meterRegistry, "skipped");
        this.error = writeThroughCounter(meterRegistry, "error");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void add(PostCreatedEvent event) {
        if (!properties.enabled()) {
            skipped.increment();
            return;
        }

        try {
            boolean allUpdated = addIfReady(event, null);
            boolean categoryUpdated = addIfReady(event, event.category());
            (allUpdated || categoryUpdated ? updated : skipped).increment();
        } catch (RuntimeException exception) {
            error.increment();
            log.warn("게시글 생성 후 최신순 ID 캐시 갱신 실패. 캐시를 무효화합니다. boardId={}",
                    event.boardId(), exception);
            invalidateSafely(event.boardId());
        }
    }

    private boolean addIfReady(PostCreatedEvent event, PostCategory category) {
        return cacheRepository.addIfReady(
                event.boardId(),
                category,
                event.postId(),
                event.createdAt(),
                properties.maxEntries(),
                properties.ttl()
        );
    }

    private void invalidateSafely(Long boardId) {
        try {
            cacheRepository.invalidateBoard(boardId);
        } catch (RuntimeException exception) {
            log.warn("최신순 ID 캐시 갱신·무효화가 모두 실패했습니다. TTL 만료로 복구합니다. boardId={}",
                    boardId, exception);
        }
    }

    private Counter writeThroughCounter(MeterRegistry meterRegistry, String result) {
        return Counter.builder("post.list.cache.write_through")
                .description("게시글 생성 후 준비된 최신순 ID 캐시 갱신")
                .tag("result", result)
                .register(meterRegistry);
    }
}
