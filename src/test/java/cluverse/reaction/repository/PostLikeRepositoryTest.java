package cluverse.reaction.repository;

import cluverse.reaction.domain.PostLike;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class PostLikeRepositoryTest {

    @Autowired
    private PostLikeRepository postLikeRepository;

    @Test
    void 게시글과_회원으로_좋아요를_조회할_수_있다() {
        // given
        postLikeRepository.save(PostLike.of(10L, 1L));

        // when
        boolean exists = postLikeRepository.existsByPostIdAndMemberId(10L, 1L);

        // then
        assertThat(exists).isTrue();
    }

    @Test
    void 회원이_좋아요한_게시글_ID_목록을_조회할_수_있다() {
        // given
        postLikeRepository.save(PostLike.of(10L, 1L));
        postLikeRepository.save(PostLike.of(20L, 1L));
        postLikeRepository.save(PostLike.of(30L, 2L));

        // when
        List<Long> postIds = postLikeRepository.findPostIdsByMemberIdAndPostIdIn(1L, List.of(10L, 20L, 30L));

        // then
        assertThat(postIds).containsExactlyInAnyOrder(10L, 20L);
    }
}
