package cluverse.meta.service.implement;

import cluverse.meta.repository.PendingViewCountRepository;
import cluverse.meta.repository.dto.ViewCountDelta;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.RedisConnectionFailureException;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ViewSurgeCleanupProcessorTest {

    @Mock
    private ViewSurgeTrackingReader viewSurgeTrackingReader;

    @Mock
    private ViewSurgeTrackingWriter viewSurgeTrackingWriter;

    @Mock
    private ViewSurgeRoutingCache viewSurgeRoutingCache;

    @Mock
    private PendingViewCountRepository pendingViewCountRepository;

    @Mock
    private PostMetaWriter postMetaWriter;

    private ViewSurgeCleanupProcessor viewSurgeCleanupProcessor;

    @BeforeEach
    void setUp() {
        viewSurgeCleanupProcessor = new ViewSurgeCleanupProcessor(
                viewSurgeTrackingReader,
                viewSurgeTrackingWriter,
                viewSurgeRoutingCache,
                pendingViewCountRepository,
                postMetaWriter,
                new SimpleMeterRegistry()
        );
    }

    @Test
    void 라우팅_선제거_후_최종_플러시_키_삭제_행_삭제_순으로_정리한다() {
        // given
        when(viewSurgeTrackingReader.readExpiredPostIds()).thenReturn(List.of(1L));
        when(pendingViewCountRepository.getAndReset(1L)).thenReturn(17L);

        // when
        viewSurgeCleanupProcessor.cleanUp();

        // then — 행을 먼저 지우면 중간 장애 시 재시도 근거가 사라진다
        InOrder inOrder = Mockito.inOrder(viewSurgeRoutingCache, pendingViewCountRepository, postMetaWriter, viewSurgeTrackingWriter);
        inOrder.verify(viewSurgeRoutingCache).remove(1L);
        inOrder.verify(pendingViewCountRepository).getAndReset(1L);
        inOrder.verify(postMetaWriter).applyViewCountDeltas(List.of(new ViewCountDelta(1L, 17L)));
        inOrder.verify(pendingViewCountRepository).delete(1L);
        inOrder.verify(viewSurgeTrackingWriter).deactivate(1L);
    }

    @Test
    void 남은_증가량이_없으면_MySQL_반영은_건너뛰고_정리만_한다() {
        // given
        when(viewSurgeTrackingReader.readExpiredPostIds()).thenReturn(List.of(1L));
        when(pendingViewCountRepository.getAndReset(1L)).thenReturn(0L);

        // when
        viewSurgeCleanupProcessor.cleanUp();

        // then
        verify(postMetaWriter, never()).applyViewCountDeltas(any());
        verify(pendingViewCountRepository).delete(1L);
        verify(viewSurgeTrackingWriter).deactivate(1L);
    }

    @Test
    void 최종_반영_실패가_확실하면_증가량을_버퍼에_되돌린다() {
        // given — 되돌리지 않으면 행이 남아도 재시도할 값이 없다
        when(viewSurgeTrackingReader.readExpiredPostIds()).thenReturn(List.of(1L));
        when(pendingViewCountRepository.getAndReset(1L)).thenReturn(5L);
        doThrow(new DataIntegrityViolationException("확실한 실패"))
                .when(postMetaWriter).applyViewCountDeltas(List.of(new ViewCountDelta(1L, 5L)));

        // when
        viewSurgeCleanupProcessor.cleanUp();

        // then
        verify(pendingViewCountRepository).restore(1L, 5L);
        verify(viewSurgeTrackingWriter, never()).deactivate(1L);
    }

    @Test
    void 커밋_여부를_알_수_없는_최종_반영_실패에서는_복구하지_않는다() {
        // given
        when(viewSurgeTrackingReader.readExpiredPostIds()).thenReturn(List.of(1L));
        when(pendingViewCountRepository.getAndReset(1L)).thenReturn(5L);
        doThrow(new QueryTimeoutException("커밋 여부 불명"))
                .when(postMetaWriter).applyViewCountDeltas(any());

        // when
        viewSurgeCleanupProcessor.cleanUp();

        // then
        verify(pendingViewCountRepository, never()).restore(anyLong(), anyLong());
        verify(viewSurgeTrackingWriter, never()).deactivate(1L);
    }

    @Test
    void 한_게시글의_정리가_실패해도_나머지는_계속_정리한다() {
        // given
        when(viewSurgeTrackingReader.readExpiredPostIds()).thenReturn(List.of(1L, 2L));
        when(pendingViewCountRepository.getAndReset(1L)).thenReturn(5L);
        doThrow(new DataIntegrityViolationException("반영 실패"))
                .when(postMetaWriter).applyViewCountDeltas(List.of(new ViewCountDelta(1L, 5L)));
        when(pendingViewCountRepository.getAndReset(2L)).thenReturn(3L);

        // when
        viewSurgeCleanupProcessor.cleanUp();

        // then
        verify(viewSurgeTrackingWriter, never()).deactivate(1L);
        verify(viewSurgeTrackingWriter).deactivate(2L);
    }

    @Test
    void 버퍼_연결_장애면_이번_주기를_조기_종료한다() {
        // given — 연결이 죽었으면 나머지도 실패한다
        when(viewSurgeTrackingReader.readExpiredPostIds()).thenReturn(List.of(1L, 2L));
        when(pendingViewCountRepository.getAndReset(1L))
                .thenThrow(new RedisConnectionFailureException("연결 실패"));

        // when
        viewSurgeCleanupProcessor.cleanUp();

        // then
        verify(pendingViewCountRepository, never()).getAndReset(2L);
        verify(viewSurgeTrackingWriter, never()).deactivate(2L);
    }
}
