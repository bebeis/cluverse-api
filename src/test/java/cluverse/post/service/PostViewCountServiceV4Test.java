package cluverse.post.service;

import cluverse.common.exception.NotFoundException;
import cluverse.meta.service.implement.PostMetaWriter;
import cluverse.meta.service.implement.ViewCountBufferWriter;
import cluverse.meta.service.implement.ViewSurgeDetector;
import cluverse.meta.service.implement.ViewSurgeRoutingCache;
import cluverse.post.service.implement.PostAccessReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostViewCountServiceV4Test {

    @Mock
    private PostAccessReader postAccessReader;

    @Mock
    private ViewSurgeRoutingCache viewSurgeRoutingCache;

    @Mock
    private ViewCountBufferWriter viewCountBufferWriter;

    @Mock
    private PostMetaWriter postMetaWriter;

    @Mock
    private ViewSurgeDetector viewSurgeDetector;

    @InjectMocks
    private PostViewCountServiceV4 postViewCountService;

    @Test
    void 추적_중이_아니면_MySQL_원자적_UPDATE_후_감지기를_거친다() {
        // given
        when(viewSurgeRoutingCache.contains(10L)).thenReturn(false);
        when(postMetaWriter.increaseViewCountAndGet(10L)).thenReturn(101L);

        // when
        postViewCountService.increaseViewCount(10L);

        // then
        verify(postAccessReader).validateActivePost(10L);
        verify(postMetaWriter).increaseViewCountAndGet(10L);
        verify(viewSurgeDetector).observe(10L, 101L);
        verifyNoInteractions(viewCountBufferWriter);
    }

    @Test
    void 추적_중이면_버퍼_경로로_빠지고_MySQL을_갱신하지_않는다() {
        // given
        when(viewSurgeRoutingCache.contains(10L)).thenReturn(true);
        when(viewCountBufferWriter.tryIncrease(10L)).thenReturn(true);

        // when
        postViewCountService.increaseViewCount(10L);

        // then
        verify(viewCountBufferWriter).tryIncrease(10L);
        verify(postMetaWriter, never()).increaseViewCountAndGet(10L);
        verifyNoInteractions(viewSurgeDetector);
    }

    @Test
    void 추적_중이라도_버퍼가_실패하면_MySQL_직접_경로로_폴백한다() {
        // given
        when(viewSurgeRoutingCache.contains(10L)).thenReturn(true);
        when(viewCountBufferWriter.tryIncrease(10L)).thenReturn(false);
        when(postMetaWriter.increaseViewCountAndGet(10L)).thenReturn(102L);

        // when
        postViewCountService.increaseViewCount(10L);

        // then
        verify(postMetaWriter).increaseViewCountAndGet(10L);
        verify(viewSurgeDetector).observe(10L, 102L);
    }

    @Test
    void 게시글이_없으면_조회수를_증가시키지_않는다() {
        // given
        doThrow(new NotFoundException("존재하지 않는 게시글입니다."))
                .when(postAccessReader).validateActivePost(10L);

        // when // then
        assertThatThrownBy(() -> postViewCountService.increaseViewCount(10L))
                .isInstanceOf(NotFoundException.class);
        verifyNoInteractions(viewSurgeRoutingCache, viewCountBufferWriter, postMetaWriter, viewSurgeDetector);
    }
}
