package cluverse.popularity.service.implement;

import cluverse.meta.repository.PendingViewCountRepository;
import cluverse.meta.repository.dto.ViewCountDelta;
import cluverse.meta.service.implement.PostMetaWriter;
import cluverse.meta.service.implement.WriteBackFailurePolicy;
import cluverse.popularity.domain.PopularPost;
import cluverse.popularity.properties.PopularityProperties;
import cluverse.popularity.repository.PopularPostRepository;
import cluverse.popularity.repository.dto.PopularitySnapshot;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class PopularityFinalizationProcessor {

    private static final int CLAIM_TIMEOUT_MULTIPLIER = 3;

    private final PopularPostRepository popularPostRepository;
    private final PendingViewCountRepository pendingViewCountRepository;
    private final PostMetaWriter postMetaWriter;
    private final PopularitySnapshotReader popularitySnapshotReader;
    private final PopularPostWriter popularPostWriter;
    private final PopularityFinalizationClaimWriter popularityFinalizationClaimWriter;
    private final PopularityProperties properties;
    private final Clock clock;
    private final MeterRegistry meterRegistry;

    public int finalizeDue() {
        LocalDateTime now = now();
        List<Long> postIds = popularPostRepository.findDuePostIdsForFinalization(
                now,
                properties.finalizationBatchSize()
        );
        if (postIds.isEmpty()) {
            return 0;
        }
        List<PopularPost> targets = popularPostRepository.findDueForFinalization(postIds, now);
        Map<Long, List<PopularPost>> targetsByPostId = new LinkedHashMap<>();
        for (PopularPost target : targets) {
            targetsByPostId.computeIfAbsent(target.getPostId(), ignored -> new java.util.ArrayList<>()).add(target);
        }
        int finalized = 0;
        for (Map.Entry<Long, List<PopularPost>> entry : targetsByPostId.entrySet()) {
            finalized += finalizePost(entry.getKey(), entry.getValue(), now);
        }
        return finalized;
    }

    private int finalizePost(Long postId, List<PopularPost> targets, LocalDateTime now) {
        LocalDateTime claimedAt = now();
        LocalDateTime staleBefore = claimedAt.minus(
                properties.finalizationInterval().multipliedBy(CLAIM_TIMEOUT_MULTIPLIER)
        );
        String claimToken = UUID.randomUUID().toString();
        if (!popularityFinalizationClaimWriter.claim(postId, claimToken, claimedAt, staleBefore)) {
            return 0;
        }

        try {
            long pending;
            try {
                pending = pendingViewCountRepository.getAndReset(postId);
            } catch (DataAccessException exception) {
                log.warn("인기글 최종화 pending 조회 실패: postId={}", postId, exception);
                return 0;
            }

            if (pending > 0 && !applyPending(postId, pending)) {
                return 0;
            }

            PopularitySnapshot snapshot = popularitySnapshotReader.read(postId);
            if (snapshot == null) {
                return 0;
            }
            int finalized = 0;
            for (PopularPost target : targets) {
                if (popularPostWriter.finalizeSnapshot(target.getId(), snapshot, now)) {
                    finalized++;
                    recordDelay(target, now);
                }
            }
            return finalized;
        } finally {
            popularityFinalizationClaimWriter.release(postId, claimToken);
        }
    }

    private void recordDelay(PopularPost target, LocalDateTime now) {
        Duration delay = Duration.between(target.getFinalizeAt(), now);
        meterRegistry.timer(
                "popularity.finalization.delay",
                "version", target.getAlgorithmVersion().name()
        ).record(delay.isNegative() ? Duration.ZERO : delay);
    }

    private boolean applyPending(Long postId, long pending) {
        try {
            postMetaWriter.applyViewCountDeltas(List.of(new ViewCountDelta(postId, pending)));
            return true;
        } catch (DataAccessException exception) {
            if (WriteBackFailurePolicy.isRollbackCertain(exception)) {
                try {
                    pendingViewCountRepository.restore(postId, pending);
                } catch (DataAccessException restoreException) {
                    exception.addSuppressed(restoreException);
                    recordPendingLossRisk("RESTORE_FAILED");
                }
            } else {
                recordPendingLossRisk("ROLLBACK_UNCERTAIN");
            }
            log.warn("인기글 최종화 pending 반영 실패: postId={}", postId, exception);
            return false;
        }
    }

    private void recordPendingLossRisk(String reason) {
        meterRegistry.counter(
                "popularity.finalization.pending.loss.risk",
                "reason", reason
        ).increment();
    }

    private LocalDateTime now() {
        return LocalDateTime.ofInstant(clock.instant(), clock.getZone());
    }
}
