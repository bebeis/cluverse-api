package cluverse.post.service.implement;

import cluverse.common.exception.ForbiddenException;
import cluverse.post.domain.Post;
import cluverse.post.domain.PostCategory;
import cluverse.post.repository.PostRepository;
import cluverse.post.service.request.PostUpdateRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostWriterTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private PostAccessReader postAccessReader;

    @InjectMocks
    private PostWriter postWriter;

    @Test
    void 작성자는_게시글을_수정한다() {
        Post post = createPost(10L, 1L);
        PostUpdateRequest request = updateRequest();
        when(postAccessReader.readOrThrow(10L)).thenReturn(post);

        postWriter.update(1L, 10L, request);

        assertThat(post.getTitle()).isEqualTo("수정 제목");
        assertThat(post.getContent()).isEqualTo("수정 본문");
    }

    @Test
    void 작성자가_아니면_게시글을_수정할_수_없다() {
        Post post = createPost(10L, 1L);
        when(postAccessReader.readOrThrow(10L)).thenReturn(post);

        assertThatThrownBy(() -> postWriter.update(2L, 10L, updateRequest()))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void 작성자는_게시글을_삭제한다() {
        Post post = createPost(10L, 1L);
        when(postAccessReader.readOrThrow(10L)).thenReturn(post);

        postWriter.delete(1L, 10L);

        assertThat(post.isActive()).isFalse();
    }

    @Test
    void 작성자가_아니면_게시글을_삭제할_수_없다() {
        Post post = createPost(10L, 1L);
        when(postAccessReader.readOrThrow(10L)).thenReturn(post);

        assertThatThrownBy(() -> postWriter.delete(2L, 10L))
                .isInstanceOf(ForbiddenException.class);
    }

    private PostUpdateRequest updateRequest() {
        return new PostUpdateRequest(
                "수정 제목",
                "수정 본문",
                PostCategory.INFORMATION,
                List.of("spring"),
                false,
                false,
                true,
                List.of()
        );
    }

    private Post createPost(Long postId, Long memberId) {
        Post post = Post.createByMember(
                List.of("spring", "backend"),
                List.of("https://cdn.example.com/posts/%d.png".formatted(postId)),
                3L,
                memberId,
                "원본 글",
                "본문 내용입니다.",
                PostCategory.INFORMATION,
                false,
                false,
                true,
                "127.0.0.1"
        );
        ReflectionTestUtils.setField(post, "id", postId);
        return post;
    }
}
