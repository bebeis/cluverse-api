package cluverse.popularity.repository;

import cluverse.popularity.domain.PopularityAlgorithmVersion;
import cluverse.popularity.domain.PopularityTrigger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PopularPostRepositoryTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 1, 12, 0);

    @Autowired
    private PopularPostRepository popularPostRepository;

    @Test
    void 중복_이벤트는_인기글을_한_행으로_UPSERT한다() {
        // when
        promote(PopularityAlgorithmVersion.V2, 1L);
        promote(PopularityAlgorithmVersion.V2, 1L);

        // then
        assertThat(popularPostRepository.findAll()).hasSize(1);
    }

    private void promote(PopularityAlgorithmVersion version, Long postId) {
        popularPostRepository.upsertPromotion(
                version,
                postId,
                10L,
                NOW,
                NOW.plusHours(47),
                120,
                PopularityTrigger.MANUAL,
                100
        );
    }
}
