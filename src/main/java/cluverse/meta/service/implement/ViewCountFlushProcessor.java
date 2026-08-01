package cluverse.meta.service.implement;

import cluverse.meta.properties.ViewSurgeProperties;
import cluverse.meta.repository.PendingViewCountRepository;
import cluverse.meta.repository.dto.ViewCountDelta;
import cluverse.popularity.domain.PopularityTrigger;
import cluverse.popularity.service.implement.PopularityPromotionInvoker;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Write-back 플러시 (조회수 증가 API V4).
 * 추적 중인 게시글만 순회한다 — Redis 전체 키를 SCAN하지 않는다.
 * get-and-reset이 원자적이라 다중 인스턴스 동시 플러시에 분산 락이 필요 없다.
 */
@Component
@Slf4j
public class ViewCountFlushProcessor {

    // 파이프라인 1회에 담는 키 수 — 한 번에 전부 보내면 Redis 이벤트 루프를 오래 막는다
    private static final int REDIS_PIPELINE_CHUNK_SIZE = 1_000;

    private final ViewSurgeTrackingReader viewSurgeTrackingReader;
    private final ViewSurgeTrackingWriter viewSurgeTrackingWriter;
    private final PendingViewCountRepository pendingViewCountRepository;
    private final PostMetaWriter postMetaWriter;
    private final PopularityPromotionInvoker popularityPromotionInvoker;
    private final ViewSurgeProperties properties;
    private final Timer flushTimer;
    private final DistributionSummary batchSizeSummary;
    private final Counter restoredCounter;
    private final Counter extensionCounter;
    private final Counter redisFallbackCounter;

    public ViewCountFlushProcessor(
            ViewSurgeTrackingReader viewSurgeTrackingReader,
            ViewSurgeTrackingWriter viewSurgeTrackingWriter,
            PendingViewCountRepository pendingViewCountRepository,
            PostMetaWriter postMetaWriter,
            PopularityPromotionInvoker popularityPromotionInvoker,
            ViewSurgeProperties properties,
            MeterRegistry meterRegistry
    ) {
        this.viewSurgeTrackingReader = viewSurgeTrackingReader;
        this.viewSurgeTrackingWriter = viewSurgeTrackingWriter;
        this.pendingViewCountRepository = pendingViewCountRepository;
        this.postMetaWriter = postMetaWriter;
        this.popularityPromotionInvoker = popularityPromotionInvoker;
        this.properties = properties;
        this.flushTimer = meterRegistry.timer("view_surge.flush.duration");
        this.batchSizeSummary = meterRegistry.summary("view_surge.flush.batch_size");
        this.restoredCounter = meterRegistry.counter("view_surge.flush.restored");
        this.extensionCounter = meterRegistry.counter("view_surge.extension");
        this.redisFallbackCounter = meterRegistry.counter("view_count.redis_fallback", "origin", "flush");
    }

    public void flush() {
        flushTimer.record(this::flushInternal);
    }

    private void flushInternal() {
        List<Long> trackedPostIds = viewSurgeTrackingReader.readActivePostIds();
        if (trackedPostIds.isEmpty()) {
            return;
        }
        List<ViewCountDelta> deltas = collectDeltas(trackedPostIds);
        if (deltas.isEmpty()) {
            return;
        }
        batchSizeSummary.record(deltas.size());
        applyOrRestore(deltas);
        popularityPromotionInvoker.tryEvaluateAll(
                deltas.stream().map(ViewCountDelta::postId).toList(),
                PopularityTrigger.VIEW_WRITE_BACK
        );
        extendSustained(deltas);
    }

    private List<ViewCountDelta> collectDeltas(List<Long> postIds) {
        List<ViewCountDelta> deltas = new ArrayList<>();
        for (int from = 0; from < postIds.size(); from += REDIS_PIPELINE_CHUNK_SIZE) {
            List<Long> chunk = postIds.subList(from, Math.min(from + REDIS_PIPELINE_CHUNK_SIZE, postIds.size()));
            try {
                List<Long> values = pendingViewCountRepository.getAndResetAll(chunk);
                for (int i = 0; i < chunk.size(); i++) {
                    if (values.get(i) > 0) {
                        deltas.add(new ViewCountDelta(chunk.get(i), values.get(i)));
                    }
                }
            } catch (DataAccessException exception) {
                // Redis가 죽었으면 나머지 청크도 실패한다 — 이번 주기를 조기 종료한다
                redisFallbackCounter.increment();
                log.warn("미반영 증가량 파이프라인 실패 — 이번 주기 잔여 청크를 중단한다. 남은 대상 {}건",
                        postIds.size() - from, exception);
                break;
            }
        }
        return deltas;
    }

    private void applyOrRestore(List<ViewCountDelta> deltas) {
        try {
            postMetaWriter.applyViewCountDeltas(deltas);
        } catch (DataAccessException exception) {
            if (WriteBackFailurePolicy.isRollbackCertain(exception)) {
                deltas.forEach(delta -> pendingViewCountRepository.restore(delta.postId(), delta.delta()));
                restoredCounter.increment(deltas.size());
            }
            throw exception;
        }
    }

    private void extendSustained(List<ViewCountDelta> deltas) {
        List<Long> sustainedPostIds = deltas.stream()
                .filter(delta -> delta.delta() >= properties.sustainThreshold())
                .map(ViewCountDelta::postId)
                .toList();
        if (sustainedPostIds.isEmpty()) {
            return;
        }
        int extendedRowCount = viewSurgeTrackingWriter.extendAll(sustainedPostIds);
        extensionCounter.increment(extendedRowCount);
    }

}
