package cluverse.meta.repository;

import cluverse.meta.domain.PostCommentCount;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class PostCommentCountRepositoryTest {

    @Autowired
    private PostCommentCountRepository postCommentCountRepository;

    @Test
    void 게시글_ID로_댓글_수를_조회할_수_있다() {
        // given
        postCommentCountRepository.save(PostCommentCount.of(10L, 2));

        // then
        assertThat(postCommentCountRepository.findByPostIdForUpdate(10L))
                .get()
                .extracting(PostCommentCount::getCommentCount)
                .isEqualTo(2);
    }

    @Test
    void 댓글_수가_0이면_row를_삭제할_수_있다() {
        // given
        postCommentCountRepository.save(PostCommentCount.of(10L, 0));

        // when
        postCommentCountRepository.deleteIfZero(10L);

        // then
        assertThat(postCommentCountRepository.findById(10L)).isEmpty();
    }
}
