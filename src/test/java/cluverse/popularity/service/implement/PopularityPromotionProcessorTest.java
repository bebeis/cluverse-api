package cluverse.popularity.service.implement;

import cluverse.popularity.domain.PopularityAlgorithmVersion;
import cluverse.popularity.domain.PopularityTrigger;
import cluverse.popularity.properties.PopularityProperties;
import cluverse.popularity.repository.dto.PopularitySnapshot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PopularityPromotionProcessorTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    private static final Instant NOW = Instant.parse("2026-08-01T03:00:00Z");

    @Mock
    private PopularitySnapshotReader popularitySnapshotReader;

    @Mock
    private PopularityPolicyReader popularityPolicyReader;

    @Mock
    private PopularPostWriter popularPostWriter;

    @Mock
    private PopularityMetricsRecorder popularityMetricsRecorder;

    @Test
    void 게시판별_기준을_넘은_변경_게시글_하나만_승격한다() {
        PopularitySnapshot snapshot = new PopularitySnapshot(
                1L, 10L, LocalDateTime.ofInstant(NOW, ZONE).minusHours(1), 7, 0);
        PopularityPolicy policy = new PopularityPolicy(20);
        given(popularitySnapshotReader.read(1L)).willReturn(snapshot);
        given(popularityPolicyReader.read(10L)).willReturn(policy);
        PopularityPromotionProcessor processor = processor();

        processor.evaluate(1L, PopularityTrigger.LIKE);

        verify(popularPostWriter).promote(
                PopularityAlgorithmVersion.V2, snapshot, policy, PopularityTrigger.LIKE);
    }

    @Test
    void 기준에_못_미치면_후보_상태를_남기지_않고_다음_상호작용을_기다린다() {
        PopularitySnapshot snapshot = new PopularitySnapshot(
                1L, 10L, LocalDateTime.ofInstant(NOW, ZONE).minusHours(1), 1, 0);
        given(popularitySnapshotReader.read(1L)).willReturn(snapshot);
        given(popularityPolicyReader.read(10L)).willReturn(new PopularityPolicy(20));
        PopularityPromotionProcessor processor = processor();

        processor.evaluate(1L, PopularityTrigger.COMMENT);

        verify(popularPostWriter, never()).promote(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    private PopularityPromotionProcessor processor() {
        return new PopularityPromotionProcessor(
                popularitySnapshotReader,
                popularityPolicyReader,
                popularPostWriter,
                popularityMetricsRecorder,
                new PopularityProperties(
                        100, 3, 2,
                        Duration.ofHours(48), Duration.ofDays(7), 0.98, 100, 0.3,
                        Duration.ofSeconds(30), 500
                ),
                Clock.fixed(NOW, ZONE)
        );
    }
}
