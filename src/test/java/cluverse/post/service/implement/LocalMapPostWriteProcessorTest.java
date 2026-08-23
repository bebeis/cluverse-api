package cluverse.post.service.implement;

import cluverse.board.service.implement.BoardReader;
import cluverse.member.service.implement.MemberReader;
import cluverse.meta.service.implement.PostMetaWriter;
import cluverse.place.domain.SelectedPlace;
import cluverse.post.domain.Post;
import cluverse.post.service.request.PostCreateRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LocalMapPostWriteProcessorTest {

    @Mock
    private BoardReader boardReader;
    @Mock
    private MemberReader memberReader;
    @Mock
    private PostWriter postWriter;
    @Mock
    private PostMetaWriter postMetaWriter;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @InjectMocks
    private LocalMapPostWriteProcessor processor;

    @Test
    void 게시글과_메타데이터만_저장하고_장소_검증은_커밋_후_이벤트로_요청한다() {
        PostCreateRequest request = org.mockito.Mockito.mock(PostCreateRequest.class);
        SelectedPlace selectedPlace = org.mockito.Mockito.mock(SelectedPlace.class);
        Post post = org.mockito.Mockito.mock(Post.class);
        given(request.boardId()).willReturn(2L);
        given(memberReader.isVerified(1L)).willReturn(true);
        given(postWriter.create(1L, request, "127.0.0.1", "request-id")).willReturn(post);
        given(post.getId()).willReturn(10L);

        Long result = processor.create(
                1L, "request-id", request, List.of(selectedPlace), "127.0.0.1");

        assertThat(result).isEqualTo(10L);
        verify(postMetaWriter).createViewCount(10L);
        verify(eventPublisher).publishEvent(
                new PostPlaceVerificationRequested(1L, 10L, List.of(selectedPlace)));
    }
}
