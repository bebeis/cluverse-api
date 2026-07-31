package cluverse.meta.service.implement;

import cluverse.meta.repository.PendingViewCountRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 조회수 증가를 Write-back 버퍼에 쌓는 도구 (조회수 증가 API V4).
 * 실패 시 false — 호출자가 직접 반영 경로로 폴백한다.
 */
@Component
@Slf4j
public class ViewCountBufferWriter {

    private final PendingViewCountRepository pendingViewCountRepository;
    private final Counter bufferPathCounter;
    private final Counter bufferFallbackCounter;
    // 버퍼 장애 중 요청마다 스택트레이스를 남기면 폴백보다 로그 I/O가 더 큰 부하가 된다 — 상태 전환에만 로깅
    private final AtomicBoolean bufferDown = new AtomicBoolean(false);

    public ViewCountBufferWriter(PendingViewCountRepository pendingViewCountRepository, MeterRegistry meterRegistry) {
        this.pendingViewCountRepository = pendingViewCountRepository;
        this.bufferPathCounter = meterRegistry.counter("view_count.redis_path");
        this.bufferFallbackCounter = meterRegistry.counter("view_count.redis_fallback", "origin", "request");
    }

    public boolean tryIncrease(Long postId) {
        try {
            pendingViewCountRepository.increase(postId);
            bufferPathCounter.increment();
            if (bufferDown.compareAndSet(true, false)) {
                log.info("조회수 버퍼 복구 — 버퍼 경로 재개");
            }
            return true;
        } catch (RedisConnectionFailureException | RedisSystemException | QueryTimeoutException exception) {
            bufferFallbackCounter.increment();
            if (bufferDown.compareAndSet(false, true)) {
                log.warn("조회수 버퍼 쓰기 실패 — 직접 반영 경로로 폴백. 복구까지 건별 로그는 생략한다. postId={}", postId, exception);
            }
            return false;
        }
    }
}
