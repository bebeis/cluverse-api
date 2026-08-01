package cluverse.popularity.service.implement;

import cluverse.popularity.domain.PopularityCandidate;
import cluverse.popularity.domain.PopularityTrigger;
import cluverse.popularity.properties.PopularityProperties;
import cluverse.popularity.repository.PopularityCandidateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PopularityCandidateProcessorTest {

    @Mock
    private PopularityCandidateRepository popularityCandidateRepository;

    @Mock
    private PopularityPromotionProcessorV2 popularityPromotionProcessorV2;

    @Mock
    private PopularityMetricsRecorder popularityMetricsRecorder;

    @Test
    void 도래한_후보만_제한된_배치로_재검사한다() {
        // given
        ZoneId zoneId = ZoneId.of("Asia/Seoul");
        Instant now = Instant.parse("2026-08-01T03:00:00Z");
        LocalDateTime nowLocal = LocalDateTime.ofInstant(now, zoneId);
        PopularityCandidate candidate = PopularityCandidate.register(
                1L,
                10L,
                nowLocal.minusMinutes(1),
                nowLocal.minusSeconds(1),
                nowLocal.plusHours(1)
        );
        PopularityProperties properties = properties();
        when(popularityCandidateRepository.findDueForUpdate(nowLocal, properties.candidateBatchSize()))
                .thenReturn(List.of(candidate));
        when(popularityCandidateRepository.count()).thenReturn(7L);
        PopularityCandidateProcessor processor = new PopularityCandidateProcessor(
                popularityCandidateRepository,
                popularityPromotionProcessorV2,
                properties,
                Clock.fixed(now, zoneId),
                popularityMetricsRecorder
        );

        // when
        processor.processDue();

        // then
        verify(popularityPromotionProcessorV2).evaluate(1L, PopularityTrigger.CANDIDATE_RECHECK);
        verify(popularityMetricsRecorder).candidateQueueSize(7L);
    }

    private PopularityProperties properties() {
        return new PopularityProperties(
                100L, 5, 3, 3, 2, 1,
                Duration.ofHours(48), Duration.ofDays(7), 0.98, 100, 0.30,
                Duration.ofMinutes(1), Duration.ofMinutes(1), 1_000,
                Duration.ofSeconds(30), 500,
                Duration.ofSeconds(30), 500,
                false, ""
        );
    }
}
