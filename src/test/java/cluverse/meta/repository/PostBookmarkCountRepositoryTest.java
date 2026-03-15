package cluverse.meta.repository;

import cluverse.meta.domain.PostBookmarkCount;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class PostBookmarkCountRepositoryTest {

    @Autowired
    private PostBookmarkCountRepository postBookmarkCountRepository;

    @Test
    void 게시글_ID로_북마크_수를_조회할_수_있다() {
        // given
        postBookmarkCountRepository.save(PostBookmarkCount.of(10L, 2));

        // then
        assertThat(postBookmarkCountRepository.findByPostIdForUpdate(10L))
                .get()
                .extracting(PostBookmarkCount::getBookmarkCount)
                .isEqualTo(2);
    }

    @Test
    void 북마크_수가_0이면_row를_삭제할_수_있다() {
        // given
        postBookmarkCountRepository.save(PostBookmarkCount.of(10L, 0));

        // when
        postBookmarkCountRepository.deleteIfZero(10L);

        // then
        assertThat(postBookmarkCountRepository.findById(10L)).isEmpty();
    }
}
