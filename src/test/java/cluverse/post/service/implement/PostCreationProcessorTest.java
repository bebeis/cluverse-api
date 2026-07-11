package cluverse.post.service.implement;

import cluverse.board.service.implement.BoardReader;
import cluverse.member.service.implement.MemberReader;
import cluverse.meta.service.implement.PostMetaWriter;
import cluverse.post.domain.Post;
import cluverse.post.domain.PostCategory;
import cluverse.post.service.request.PostCreateRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostCreationProcessorTest {

    @Mock
    private PostWriter postWriter;

    @Mock
    private PostMetaWriter postMetaWriter;

    @Mock
    private BoardReader boardReader;

    @Mock
    private MemberReader memberReader;

    @InjectMocks
    private PostCreationProcessor postCreationProcessor;

    @Test
    void 게시글_작성시_쓰기권한_검증후_게시글과_조회수를_생성한다() {
        // given
        PostCreateRequest request = new PostCreateRequest(
                3L,
                "제목",
                "본문",
                PostCategory.INFORMATION,
                List.of("spring"),
                false,
                false,
                true,
                List.of()
        );
        Post post = Post.createByMember(
                List.of("spring"),
                List.of(),
                3L,
                1L,
                "제목",
                "본문",
                PostCategory.INFORMATION,
                false,
                false,
                true,
                "127.0.0.1"
        );
        ReflectionTestUtils.setField(post, "id", 10L);
        when(memberReader.isVerified(1L)).thenReturn(true);
        when(postWriter.create(1L, request, "127.0.0.1")).thenReturn(post);

        // when
        Long postId = postCreationProcessor.create(1L, request, "127.0.0.1");

        // then
        assertThat(postId).isEqualTo(10L);
        InOrder inOrder = inOrder(boardReader, postWriter, postMetaWriter);
        inOrder.verify(boardReader).validateWritable(1L, true, 3L);
        inOrder.verify(postWriter).create(1L, request, "127.0.0.1");
        inOrder.verify(postMetaWriter).createViewCount(10L);
    }
}
