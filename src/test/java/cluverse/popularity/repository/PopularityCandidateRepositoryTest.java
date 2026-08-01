package cluverse.popularity.repository;

import cluverse.popularity.domain.PopularityCandidate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PopularityCandidateRepositoryTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 1, 12, 0);

    @Autowired
    private PopularityCandidateRepository popularityCandidateRepository;

    @Test
    void 중복_이벤트는_후보를_한_행으로_UPSERT한다() {
        // when
        popularityCandidateRepository.upsert(1L, 10L, NOW, NOW.plusSeconds(30), NOW.plusHours(48));
        popularityCandidateRepository.upsert(1L, 10L, NOW.plusSeconds(1), NOW.plusSeconds(10), NOW.plusHours(48));

        // then
        assertThat(popularityCandidateRepository.findAll()).hasSize(1);
        assertThat(popularityCandidateRepository.findById(1L))
                .get()
                .extracting(PopularityCandidate::getNextCheckAt)
                .isEqualTo(NOW.plusSeconds(10));
    }

    @Test
    void 재검사는_등록_시각을_보존하고_다음_검사_시각만_옮긴다() {
        // given
        popularityCandidateRepository.upsert(1L, 10L, NOW, NOW, NOW.plusHours(48));

        // when
        popularityCandidateRepository.reschedule(1L, NOW.plusSeconds(30), NOW.plusMinutes(1), NOW.plusHours(48));

        // then
        PopularityCandidate candidate = popularityCandidateRepository.findById(1L).orElseThrow();
        assertThat(candidate.getRegisteredAt()).isEqualTo(NOW);
        assertThat(candidate.getLastCheckedAt()).isEqualTo(NOW.plusSeconds(30));
        assertThat(candidate.getNextCheckAt()).isEqualTo(NOW.plusMinutes(1));
    }

    @Test
    void 도래한_후보만_인덱스_순서로_제한해_조회한다() {
        // given
        popularityCandidateRepository.upsert(1L, 10L, NOW, NOW.minusSeconds(1), NOW.plusHours(48));
        popularityCandidateRepository.upsert(2L, 10L, NOW, NOW.minusSeconds(2), NOW.plusHours(48));
        popularityCandidateRepository.upsert(3L, 10L, NOW, NOW.plusSeconds(1), NOW.plusHours(48));

        // when
        List<PopularityCandidate> due = popularityCandidateRepository.findDueForUpdate(NOW, 1);

        // then
        assertThat(due).extracting(PopularityCandidate::getPostId).containsExactly(2L);
    }
}
