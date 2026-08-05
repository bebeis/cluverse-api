package cluverse.popularity.service.implement;

import cluverse.popularity.domain.PopularPost;
import cluverse.popularity.properties.PopularityProperties;
import cluverse.popularity.repository.PopularPostRepository;
import cluverse.popularity.repository.dto.PopularitySnapshot;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class PopularityFinalizationProcessor {

    private final PopularPostRepository popularPostRepository;
    private final PopularitySnapshotReader popularitySnapshotReader;
    private final PopularPostWriter popularPostWriter;
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
            targetsByPostId.computeIfAbsent(target.getPostId(), ignored -> new ArrayList<>()).add(target);
        }
        int finalized = 0;
        for (Map.Entry<Long, List<PopularPost>> entry : targetsByPostId.entrySet()) {
            finalized += finalizePost(entry.getKey(), entry.getValue(), now);
        }
        return finalized;
    }

    private int finalizePost(Long postId, List<PopularPost> targets, LocalDateTime now) {
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
    }

    private void recordDelay(PopularPost target, LocalDateTime now) {
        Duration delay = Duration.between(target.getFinalizeAt(), now);
        meterRegistry.timer(
                "popularity.finalization.delay",
                "version", target.getAlgorithmVersion().name()
        ).record(delay.isNegative() ? Duration.ZERO : delay);
    }

    private LocalDateTime now() {
        return LocalDateTime.ofInstant(clock.instant(), clock.getZone());
    }
}
