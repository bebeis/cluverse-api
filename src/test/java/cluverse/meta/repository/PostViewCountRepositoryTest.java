package cluverse.meta.repository;

import cluverse.meta.domain.PostViewCount;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class PostViewCountRepositoryTest {

    @Autowired
    private PostViewCountRepository postViewCountRepository;

    @Test
    void 게시글_ID로_조회수_레코드를_저장할_수_있다() {
        // given
        postViewCountRepository.save(PostViewCount.of(10L, 0));

        // then
        assertThat(postViewCountRepository.findById(10L))
                .get()
                .extracting(PostViewCount::getViewCount)
                .isEqualTo(0);
    }

    @Test
    void 게시글_ID로_조회수를_증가시킬_수_있다() {
        // given
        postViewCountRepository.save(PostViewCount.of(10L, 0));

        // when
        int updatedRowCount = postViewCountRepository.increaseCount(10L);

        // then
        assertThat(updatedRowCount).isEqualTo(1);
        assertThat(postViewCountRepository.findById(10L))
                .get()
                .extracting(PostViewCount::getViewCount)
                .isEqualTo(1);
    }
}
