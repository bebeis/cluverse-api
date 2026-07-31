package cluverse.meta.service.implement;

import cluverse.meta.repository.PendingViewCountRepository;
import cluverse.meta.repository.dto.ViewCountDelta;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 급상승 종료 정리 (조회수 증가 API V4).
 * 최종 플러시 → Redis 키 삭제 → 추적 행 삭제 순서 고정 —
 * 중간에 실패해도 행이 남아 다음 주기가 재시도한다.
 */
@Component
@Slf4j
public class ViewSurgeCleanupProcessor {

    private final ViewSurgeTrackingReader viewSurgeTrackingReader;
    private final ViewSurgeTrackingWriter viewSurgeTrackingWriter;
    private final ViewSurgeRoutingCache viewSurgeRoutingCache;
    private final PendingViewCountRepository pendingViewCountRepository;
    private final PostMetaWriter postMetaWriter;
    private final Counter cleanupCounter;
    private final Counter redisFallbackCounter;

    public ViewSurgeCleanupProcessor(
            ViewSurgeTrackingReader viewSurgeTrackingReader,
            ViewSurgeTrackingWriter viewSurgeTrackingWriter,
            ViewSurgeRoutingCache viewSurgeRoutingCache,
            PendingViewCountRepository pendingViewCountRepository,
            PostMetaWriter postMetaWriter,
            MeterRegistry meterRegistry
    ) {
        this.viewSurgeTrackingReader = viewSurgeTrackingReader;
        this.viewSurgeTrackingWriter = viewSurgeTrackingWriter;
        this.viewSurgeRoutingCache = viewSurgeRoutingCache;
        this.pendingViewCountRepository = pendingViewCountRepository;
        this.postMetaWriter = postMetaWriter;
        this.cleanupCounter = meterRegistry.counter("view_surge.cleanup");
        this.redisFallbackCounter = meterRegistry.counter("view_count.redis_fallback", "origin", "cleanup");
    }

    public void cleanUp() {
        for (Long postId : viewSurgeTrackingReader.readExpiredPostIds()) {
            try {
                viewSurgeRoutingCache.remove(postId);
                flushRemaining(postId);
                pendingViewCountRepository.delete(postId);
                viewSurgeTrackingWriter.deactivate(postId);
                cleanupCounter.increment();
            } catch (RedisConnectionFailureException exception) {
                // 연결 장애면 나머지도 실패한다 — 이번 주기를 조기 종료하고 다음 주기에 재시도
                redisFallbackCounter.increment();
                log.warn("급상승 정리 중 버퍼 연결 실패 — 이번 주기를 중단한다. postId={}", postId, exception);
                return;
            } catch (DataAccessException exception) {
                log.warn("급상승 정리 실패 — 다음 주기에 재시도한다. postId={}", postId, exception);
            }
        }
    }

    private void flushRemaining(Long postId) {
        long remaining = pendingViewCountRepository.getAndReset(postId);
        if (remaining == 0) {
            return;
        }
        try {
            postMetaWriter.applyViewCountDeltas(List.of(new ViewCountDelta(postId, remaining)));
        } catch (DataAccessException exception) {
            // 이미 0으로 리셋했으므로 되돌리지 않으면 행이 남아도 재시도할 값이 없다
            if (WriteBackFailurePolicy.isRollbackCertain(exception)) {
                pendingViewCountRepository.restore(postId, remaining);
            }
            throw exception;
        }
    }
}
