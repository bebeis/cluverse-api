package cluverse.popularity.service.implement;

import cluverse.popularity.domain.PopularityCandidate;
import cluverse.popularity.properties.PopularityProperties;
import cluverse.popularity.repository.PopularityCandidateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PopularityCandidateClaimerTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 1, 12, 0);

    @Mock
    private PopularityCandidateRepository popularityCandidateRepository;

    @Test
    void 잠근_후보의_다음_검사_시각을_먼저_옮기고_claim을_반환한다() {
        // given
        PopularityCandidate first = candidate(1L, NOW.minusSeconds(2));
        PopularityCandidate second = candidate(2L, NOW.minusSeconds(1));
        PopularityProperties properties = properties();
        when(popularityCandidateRepository.findDueForUpdate(NOW, properties.candidateBatchSize()))
                .thenReturn(List.of(first, second));
        PopularityCandidateClaimer claimer = new PopularityCandidateClaimer(
                popularityCandidateRepository,
                properties
        );

        // when
        List<PopularityCandidateClaim> claims = claimer.claimDue(NOW);

        // then
        assertThat(claims).containsExactly(
                new PopularityCandidateClaim(1L, NOW.minusSeconds(2)),
                new PopularityCandidateClaim(2L, NOW.minusSeconds(1))
        );
        verify(popularityCandidateRepository).reschedule(
                1L, NOW, NOW.plusSeconds(30), NOW.plusHours(1)
        );
        verify(popularityCandidateRepository).reschedule(
                2L, NOW, NOW.plusSeconds(30), NOW.plusHours(1)
        );
    }

    private PopularityCandidate candidate(Long postId, LocalDateTime nextCheckAt) {
        return PopularityCandidate.register(
                postId,
                10L,
                NOW.minusMinutes(1),
                nextCheckAt,
                NOW.plusHours(1)
        );
    }

    private PopularityProperties properties() {
        return new PopularityProperties(
                100L, 5, 3, 3, 2, 1,
                Duration.ofHours(48), Duration.ofDays(7), 0.98, 100, 0.30,
                Duration.ofMinutes(1), Duration.ofMinutes(1), false, 1_000,
                Duration.ofSeconds(30), 500,
                Duration.ofSeconds(30), 500,
                false, ""
        );
    }
}
