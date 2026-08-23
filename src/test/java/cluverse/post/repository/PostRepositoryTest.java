package cluverse.post.repository;

import cluverse.post.domain.Post;
import cluverse.post.domain.PostCategory;
import jakarta.persistence.EntityManager;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class PostRepositoryTest {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void 장소_연결용_게시글을_조회하면_기존_장소도_함께_가져온다() {
        // given
        Post post = Post.createByMember(
                List.of(), List.of(), 1L, 1L, "제목", "본문", PostCategory.INFORMATION,
                false, false, true, "127.0.0.1"
        );
        post.addPlace(10L, 0, 1L, null, false);
        Post saved = postRepository.saveAndFlush(post);
        entityManager.clear();

        // when
        Post found = postRepository.findWithPlacesById(saved.getId()).orElseThrow();

        // then
        assertThat(Hibernate.isInitialized(found.getPlaces())).isTrue();
        assertThat(found.getPlaces()).hasSize(1);
    }
}
