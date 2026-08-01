package cluverse.popularity.service.implement;

import cluverse.popularity.domain.PopularityTrigger;
import cluverse.popularity.repository.PopularityCandidateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PopularityCandidateProcessorTest {

    @Mock
    private PopularityCandidateRepository popularityCandidateRepository;

    @Mock
    private PopularityCandidateClaimer popularityCandidateClaimer;

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
        PopularityCandidateClaim claim = new PopularityCandidateClaim(1L, nowLocal.minusSeconds(1));
        when(popularityCandidateClaimer.claimDue(nowLocal)).thenReturn(List.of(claim));
        when(popularityCandidateRepository.count()).thenReturn(7L);
        PopularityCandidateProcessor processor = new PopularityCandidateProcessor(
                popularityCandidateRepository,
                popularityCandidateClaimer,
                popularityPromotionProcessorV2,
                Clock.fixed(now, zoneId),
                popularityMetricsRecorder
        );

        // when
        int processed = processor.processDue();

        // then
        assertThat(processed).isEqualTo(1);
        verify(popularityPromotionProcessorV2).evaluate(1L, PopularityTrigger.CANDIDATE_RECHECK);
        verify(popularityMetricsRecorder).candidateLag(claim.dueAt(), nowLocal);
        verify(popularityMetricsRecorder).candidateQueueSize(7L);
    }

    @Test
    void 후보_하나의_평가가_실패해도_나머지_후보를_계속_처리한다() {
        // given
        ZoneId zoneId = ZoneId.of("Asia/Seoul");
        Instant now = Instant.parse("2026-08-01T03:00:00Z");
        LocalDateTime nowLocal = LocalDateTime.ofInstant(now, zoneId);
        when(popularityCandidateClaimer.claimDue(nowLocal)).thenReturn(List.of(
                new PopularityCandidateClaim(1L, nowLocal.minusSeconds(2)),
                new PopularityCandidateClaim(2L, nowLocal.minusSeconds(1))
        ));
        doThrow(new IllegalStateException("평가 실패"))
                .when(popularityPromotionProcessorV2)
                .evaluate(1L, PopularityTrigger.CANDIDATE_RECHECK);
        PopularityCandidateProcessor processor = new PopularityCandidateProcessor(
                popularityCandidateRepository,
                popularityCandidateClaimer,
                popularityPromotionProcessorV2,
                Clock.fixed(now, zoneId),
                popularityMetricsRecorder
        );

        // when
        int processed = processor.processDue();

        // then
        assertThat(processed).isEqualTo(1);
        verify(popularityPromotionProcessorV2).evaluate(2L, PopularityTrigger.CANDIDATE_RECHECK);
        verify(popularityMetricsRecorder).candidateEvaluationFailed();
    }
}
