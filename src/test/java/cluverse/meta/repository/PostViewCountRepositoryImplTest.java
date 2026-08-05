package cluverse.meta.repository;

import cluverse.meta.domain.PostViewCount;
import cluverse.meta.repository.dto.ViewCountDelta;
import cluverse.meta.repository.dto.ViewCountSnapshot;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PostViewCountRepositoryImplTest {

    @Autowired
    private PostViewCountRepository postViewCountRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void 증가량_배치를_한_번에_반영한다() {
        // given
        postViewCountRepository.save(PostViewCount.of(1L, 10));
        postViewCountRepository.save(PostViewCount.of(2L, 20));
        entityManager.flush();

        // when
        postViewCountRepository.increaseByDeltas(List.of(
                new ViewCountDelta(1L, 7L),
                new ViewCountDelta(2L, 30L)
        ));
        entityManager.clear();

        // then
        assertThat(postViewCountRepository.findById(1L))
                .get()
                .extracting(PostViewCount::getViewCount)
                .isEqualTo(17L);
        assertThat(postViewCountRepository.findById(2L))
                .get()
                .extracting(PostViewCount::getViewCount)
                .isEqualTo(50L);
    }

    @Test
    void 체크포인트는_DB_조회수를_감소시키지_않는다() {
        // given
        postViewCountRepository.save(PostViewCount.of(1L, 100));
        postViewCountRepository.save(PostViewCount.of(2L, 20));
        entityManager.flush();

        // when
        postViewCountRepository.checkpointViewCounts(List.of(
                new ViewCountSnapshot(1L, 90L),
                new ViewCountSnapshot(2L, 30L)
        ));
        entityManager.clear();

        // then
        assertThat(postViewCountRepository.findById(1L))
                .get()
                .extracting(PostViewCount::getViewCount)
                .isEqualTo(100L);
        assertThat(postViewCountRepository.findById(2L))
                .get()
                .extracting(PostViewCount::getViewCount)
                .isEqualTo(30L);
    }
}
