package cluverse.meta.scheduler;

import cluverse.meta.service.implement.DeltaViewCountCounter;
import cluverse.meta.service.implement.InactiveCounterEvictor;
import cluverse.meta.service.implement.LocalViewCountRecovery;
import cluverse.meta.service.implement.ViewCountCheckpointWorker;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ViewCountScheduler {

    private final DeltaViewCountCounter deltaViewCountCounter;
    private final ViewCountCheckpointWorker viewCountCheckpointWorker;
    private final InactiveCounterEvictor inactiveCounterEvictor;
    private final LocalViewCountRecovery localViewCountRecovery;
    private final MeterRegistry meterRegistry;

    @Scheduled(fixedDelayString = "${view-count.delta-flush-interval:1m}", initialDelayString = "1m")
    public void flushTimeBasedDelta() {
        run("delta_flush", deltaViewCountCounter::flushTimeBased);
    }

    @Scheduled(fixedDelayString = "${view-count.checkpoint-interval:1m}", initialDelayString = "1m")
    public void checkpointTotalCounters() {
        run("checkpoint", viewCountCheckpointWorker::checkpoint);
    }

    @Scheduled(fixedDelayString = "${view-count.checkpoint-interval:1m}", initialDelayString = "90s")
    public void evictInactiveCounters() {
        run("eviction", inactiveCounterEvictor::evict);
    }

    @Scheduled(fixedDelayString = "${view-count.checkpoint-interval:1m}", initialDelayString = "30s")
    public void recoverLocalDeltas() {
        try {
            long recovered = localViewCountRecovery.recover();
            meterRegistry.counter("view_count.local.recovered").increment(recovered);
        } catch (RuntimeException exception) {
            log.debug("Redis 미복구 상태이거나 로컬 조회수 복구에 실패했습니다.", exception);
        }
    }

    private void run(String operation, MeasuredWork work) {
        try {
            Integer processed = meterRegistry.timer("view_count.worker.duration", "operation", operation)
                    .record(work::run);
            meterRegistry.summary("view_count.worker.processed", "operation", operation)
                    .record(processed == null ? 0 : processed);
        } catch (RuntimeException exception) {
            meterRegistry.counter("view_count.worker.failures", "operation", operation).increment();
            log.warn("조회수 백그라운드 작업 실패: operation={}", operation, exception);
        }
    }

    @FunctionalInterface
    private interface MeasuredWork {
        int run();
    }
}
