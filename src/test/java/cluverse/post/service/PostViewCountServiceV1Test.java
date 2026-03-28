package cluverse.post.service;

import cluverse.meta.service.implement.PostMetaWriter;
import cluverse.post.domain.Post;
import cluverse.post.domain.PostCategory;
import cluverse.post.service.implement.PostAccessReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostViewCountServiceV1Test {

    @Mock
    private PostAccessReader postAccessReader;

    @Mock
    private PostMetaWriter postMetaWriter;

    @InjectMocks
    private PostViewCountServiceV1 postViewCountService;

    @Test
    void V1_조회수_증가시_기존_메타_서비스에게_위임한다() {
        // given
        when(postAccessReader.readOrThrow(10L)).thenReturn(createPost(10L));

        // when
        postViewCountService.increaseViewCountV1(10L);

        // then
        verify(postAccessReader).readOrThrow(10L);
        verify(postMetaWriter).increaseViewCount(10L);
    }

    @Test
    void V2_조회수_증가시_낙관적_락_메타_서비스에게_위임한다() {
        // given
        when(postAccessReader.readOrThrow(10L)).thenReturn(createPost(10L));

        // when
        postViewCountService.increaseViewCountV2(10L);

        // then
        verify(postAccessReader).readOrThrow(10L);
        verify(postMetaWriter).increaseViewCountV2(10L);
    }

    private Post createPost(Long postId) {
        Post post = Post.createByMember(
                List.of("spring"),
                List.of(),
                3L,
                1L,
                "조회수 비교용 게시글",
                "본문",
                PostCategory.INFORMATION,
                false,
                false,
                true,
                "127.0.0.1"
        );
        ReflectionTestUtils.setField(post, "id", postId);
        ReflectionTestUtils.setField(post, "createdAt", LocalDateTime.of(2026, 3, 13, 14, 0));
        ReflectionTestUtils.setField(post, "updatedAt", LocalDateTime.of(2026, 3, 13, 14, 0));
        return post;
    }
}
