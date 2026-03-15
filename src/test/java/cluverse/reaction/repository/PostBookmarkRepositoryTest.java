package cluverse.reaction.repository;

import cluverse.reaction.domain.PostBookmark;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class PostBookmarkRepositoryTest {

    @Autowired
    private PostBookmarkRepository postBookmarkRepository;

    @Test
    void 게시글과_회원으로_북마크를_조회할_수_있다() {
        // given
        postBookmarkRepository.save(PostBookmark.of(1L, 10L));

        // when
        boolean exists = postBookmarkRepository.existsByMemberIdAndPostId(1L, 10L);

        // then
        assertThat(exists).isTrue();
    }

    @Test
    void 회원이_북마크한_게시글_ID_목록을_조회할_수_있다() {
        // given
        postBookmarkRepository.save(PostBookmark.of(1L, 10L));
        postBookmarkRepository.save(PostBookmark.of(1L, 20L));
        postBookmarkRepository.save(PostBookmark.of(2L, 30L));

        // when
        List<Long> postIds = postBookmarkRepository.findPostIdsByMemberIdAndPostIdIn(1L, List.of(10L, 20L, 30L));

        // then
        assertThat(postIds).containsExactlyInAnyOrder(10L, 20L);
    }
}
