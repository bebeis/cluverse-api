package cluverse.popularity.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PopularityFinalizationClaimRepositoryTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 1, 12, 0);

    @Autowired
    private PopularityFinalizationClaimRepository repository;

    @Test
    void 활성_claim은_다른_워커가_덮어쓰지_못한다() {
        // given
        repository.tryClaim(1L, "worker-a", NOW, NOW.minusMinutes(1));

        // when
        repository.tryClaim(1L, "worker-b", NOW.plusSeconds(10), NOW.minusSeconds(50));

        // then
        assertThat(repository.existsByPostIdAndClaimToken(1L, "worker-a")).isTrue();
        assertThat(repository.existsByPostIdAndClaimToken(1L, "worker-b")).isFalse();
    }

    @Test
    void 만료된_claim은_다른_워커가_인계한다() {
        // given
        repository.tryClaim(1L, "worker-a", NOW.minusMinutes(2), NOW.minusMinutes(3));

        // when
        repository.tryClaim(1L, "worker-b", NOW, NOW.minusMinutes(1));

        // then
        assertThat(repository.existsByPostIdAndClaimToken(1L, "worker-b")).isTrue();
    }
}
