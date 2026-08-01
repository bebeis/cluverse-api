package cluverse.popularity.service.implement;

import cluverse.popularity.domain.PopularityAlgorithmVersion;
import cluverse.popularity.properties.PopularityProperties;
import cluverse.popularity.repository.dto.PopularitySnapshot;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PopularityBatchProcessorV1 {

    private final PopularitySnapshotReader popularitySnapshotReader;
    private final PopularityPromotionProcessorV2 popularityPromotionProcessorV2;
    private final PopularityProperties properties;
    private final Clock clock;
    private final MeterRegistry meterRegistry;

    public int run() {
        return meterRegistry.timer("popularity.batch.duration", "version", PopularityAlgorithmVersion.V1.name())
                .record(this::scanAll);
    }

    private int scanAll() {
        LocalDateTime createdFrom = LocalDateTime.ofInstant(clock.instant(), clock.getZone())
                .minus(properties.promotionWindow());
        LocalDateTime lastCreatedAt = createdFrom;
        long lastPostId = 0L;
        int examined = 0;
        while (true) {
            List<PopularitySnapshot> chunk = popularitySnapshotReader.readRecentAfter(
                    createdFrom,
                    lastCreatedAt,
                    lastPostId,
                    properties.scanChunkSize()
            );
            if (chunk.isEmpty()) {
                break;
            }
            for (PopularitySnapshot snapshot : chunk) {
                popularityPromotionProcessorV2.evaluateBaseline(snapshot);
            }
            examined += chunk.size();
            meterRegistry.summary("popularity.batch.size", "version", PopularityAlgorithmVersion.V1.name())
                    .record(chunk.size());
            PopularitySnapshot last = chunk.getLast();
            lastCreatedAt = last.createdAt();
            lastPostId = last.postId();
            if (chunk.size() < properties.scanChunkSize()) {
                break;
            }
        }
        return examined;
    }
}
