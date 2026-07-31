package cluverse.meta.repository;

import cluverse.meta.domain.PostViewCount;
import cluverse.meta.repository.dto.ViewCountDelta;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import java.util.List;
import java.util.OptionalLong;

import static org.assertj.core.api.Assertions.assertThat;

// LAST_INSERT_ID(expr)는 MySQL 문법 — 임베디드 교체 대신 MODE=MySQL H2 설정을 그대로 쓴다
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PostViewCountRepositoryImplTest {

    @Autowired
    private PostViewCountRepository postViewCountRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void 증가와_동시에_증가_후_누적값을_돌려준다() {
        // given
        postViewCountRepository.save(PostViewCount.of(10L, 100));
        entityManager.flush();

        // when // then
        assertThat(postViewCountRepository.increaseAndGet(10L)).isEqualTo(OptionalLong.of(101L));
        assertThat(postViewCountRepository.increaseAndGet(10L)).isEqualTo(OptionalLong.of(102L));

        entityManager.clear();
        assertThat(postViewCountRepository.findById(10L))
                .get()
                .extracting(PostViewCount::getViewCount)
                .isEqualTo(102);
    }

    @Test
    void 조회수_레코드가_없으면_빈_값을_돌려준다() {
        // when // then
        assertThat(postViewCountRepository.increaseAndGet(99L)).isEqualTo(OptionalLong.empty());
    }

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
                .isEqualTo(17);
        assertThat(postViewCountRepository.findById(2L))
                .get()
                .extracting(PostViewCount::getViewCount)
                .isEqualTo(50);
    }
}
