package cluverse.popularity.service.implement;

import cluverse.popularity.properties.PopularityProperties;
import cluverse.popularity.repository.dto.PopularitySnapshot;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PopularityBatchProcessorV1Test {

    @Mock
    private PopularitySnapshotReader popularitySnapshotReader;

    @Mock
    private PopularityPromotionProcessorV2 popularityPromotionProcessorV2;

    @Test
    void 최근_게시글을_postId_키셋으로_끝까지_순회한다() {
        ZoneId zoneId = ZoneId.of("Asia/Seoul");
        Instant now = Instant.parse("2026-08-01T03:00:00Z");
        LocalDateTime createdFrom = LocalDateTime.ofInstant(now, zoneId).minusHours(48);
        PopularitySnapshot first = snapshot(1L, createdFrom.plusMinutes(1));
        PopularitySnapshot second = snapshot(2L, createdFrom.plusMinutes(2));
        PopularitySnapshot third = snapshot(3L, createdFrom.plusMinutes(3));
        PopularityProperties properties = properties(2);
        when(popularitySnapshotReader.readRecentAfter(createdFrom, createdFrom, 0L, 2))
                .thenReturn(List.of(first, second));
        when(popularitySnapshotReader.readRecentAfter(createdFrom, second.createdAt(), 2L, 2))
                .thenReturn(List.of(third));
        PopularityBatchProcessorV1 processor = new PopularityBatchProcessorV1(
                popularitySnapshotReader,
                popularityPromotionProcessorV2,
                properties,
                Clock.fixed(now, zoneId),
                new SimpleMeterRegistry()
        );

        int examined = processor.run();

        assertThat(examined).isEqualTo(3);
        InOrder inOrder = inOrder(popularityPromotionProcessorV2);
        inOrder.verify(popularityPromotionProcessorV2).evaluateBaseline(first);
        inOrder.verify(popularityPromotionProcessorV2).evaluateBaseline(second);
        inOrder.verify(popularityPromotionProcessorV2).evaluateBaseline(third);
    }

    private PopularitySnapshot snapshot(Long postId, LocalDateTime createdAt) {
        return new PopularitySnapshot(postId, 10L, createdAt, 5, 3, 100);
    }

    private PopularityProperties properties(int scanChunkSize) {
        return new PopularityProperties(
                100L, 5, 3, 3, 2, 1,
                Duration.ofHours(48), Duration.ofDays(7), 0.98, 100, 0.30,
                Duration.ofMinutes(1), Duration.ofMinutes(1), scanChunkSize,
                Duration.ofSeconds(30), 500,
                Duration.ofSeconds(30), 500,
                false, ""
        );
    }
}
