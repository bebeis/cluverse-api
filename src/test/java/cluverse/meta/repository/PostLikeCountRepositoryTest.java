package cluverse.meta.repository;

import cluverse.meta.domain.PostLikeCount;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class PostLikeCountRepositoryTest {

    @Autowired
    private PostLikeCountRepository postLikeCountRepository;

    @Test
    void 게시글_ID로_좋아요_수를_조회할_수_있다() {
        // given
        postLikeCountRepository.save(PostLikeCount.of(10L, 2));

        // then
        assertThat(postLikeCountRepository.findByPostIdForUpdate(10L))
                .get()
                .extracting(PostLikeCount::getLikeCount)
                .isEqualTo(2);
    }
}
