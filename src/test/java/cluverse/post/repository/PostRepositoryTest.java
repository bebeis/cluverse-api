package cluverse.post.repository;

import cluverse.post.domain.Post;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class PostRepositoryTest {

    @Autowired
    private PostRepository postRepository;

    @Test
    void 활성_게시글의_조회수를_update_쿼리로_증가시킨다() {
        // given
        Post post = postRepository.save(createPost("활성 게시글"));

        // when
        int updatedRowCount = postRepository.increaseViewCount(post.getId());

        // then
        assertThat(updatedRowCount).isEqualTo(1);
        assertThat(postRepository.findById(post.getId()))
                .get()
                .extracting(Post::getViewCount)
                .isEqualTo(1);
    }

    @Test
    void 삭제된_게시글은_조회수를_증가시키지_않는다() {
        // given
        Post post = createPost("삭제된 게시글");
        post.delete();
        post = postRepository.save(post);

        // when
        int updatedRowCount = postRepository.increaseViewCount(post.getId());

        // then
        assertThat(updatedRowCount).isZero();
        assertThat(postRepository.findById(post.getId()))
                .get()
                .extracting(Post::getViewCount)
                .isEqualTo(0);
    }

    @Test
    void 활성_게시글의_좋아요_수를_update_쿼리로_증가시킨다() {
        // given
        Post post = postRepository.save(createPost("좋아요 게시글"));

        // when
        int updatedRowCount = postRepository.increaseLikeCount(post.getId());

        // then
        assertThat(updatedRowCount).isEqualTo(1);
        assertThat(postRepository.findById(post.getId()))
                .get()
                .extracting(Post::getLikeCount)
                .isEqualTo(1);
    }

    @Test
    void 좋아요_수가_0이면_감소시키지_않는다() {
        // given
        Post post = postRepository.save(createPost("좋아요 0 게시글"));

        // when
        int updatedRowCount = postRepository.decreaseLikeCount(post.getId());

        // then
        assertThat(updatedRowCount).isZero();
        assertThat(postRepository.findById(post.getId()))
                .get()
                .extracting(Post::getLikeCount)
                .isEqualTo(0);
    }

    @Test
    void 활성_게시글의_북마크_수를_update_쿼리로_증가시킨다() {
        // given
        Post post = postRepository.save(createPost("북마크 게시글"));

        // when
        int updatedRowCount = postRepository.increaseBookmarkCount(post.getId());

        // then
        assertThat(updatedRowCount).isEqualTo(1);
        assertThat(postRepository.findById(post.getId()))
                .get()
                .extracting(Post::getBookmarkCount)
                .isEqualTo(1);
    }

    @Test
    void 북마크_수가_0이면_감소시키지_않는다() {
        // given
        Post post = postRepository.save(createPost("북마크 0 게시글"));

        // when
        int updatedRowCount = postRepository.decreaseBookmarkCount(post.getId());

        // then
        assertThat(updatedRowCount).isZero();
        assertThat(postRepository.findById(post.getId()))
                .get()
                .extracting(Post::getBookmarkCount)
                .isEqualTo(0);
    }

    private Post createPost(String title) {
        Post post = Post.createByMember(
                java.util.List.of("spring", "backend"),
                java.util.List.of("https://cdn.example.com/posts/1.png"),
                3L,
                1L,
                title,
                "본문 내용입니다.",
                cluverse.post.domain.PostCategory.INFORMATION,
                false,
                false,
                true,
                "127.0.0.1"
        );
        ReflectionTestUtils.setField(post, "viewCount", 0);
        ReflectionTestUtils.setField(post, "likeCount", 0);
        ReflectionTestUtils.setField(post, "bookmarkCount", 0);
        return post;
    }
}
