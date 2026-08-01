package cluverse.meta.service.implement;

import cluverse.meta.properties.ViewSurgeProperties;
import cluverse.meta.repository.PendingViewCountRepository;
import cluverse.meta.repository.dto.ViewCountDelta;
import cluverse.popularity.service.implement.PopularityPromotionInvoker;
import cluverse.popularity.domain.PopularityTrigger;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.RedisConnectionFailureException;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ViewCountFlushProcessorTest {

    private static final long SUSTAIN_THRESHOLD = 100L;

    @Mock
    private ViewSurgeTrackingReader viewSurgeTrackingReader;

    @Mock
    private ViewSurgeTrackingWriter viewSurgeTrackingWriter;

    @Mock
    private PendingViewCountRepository pendingViewCountRepository;

    @Mock
    private PostMetaWriter postMetaWriter;

    @Mock
    private PopularityPromotionInvoker popularityPromotionInvoker;

    private ViewCountFlushProcessor viewCountFlushProcessor;

    @BeforeEach
    void setUp() {
        viewCountFlushProcessor = new ViewCountFlushProcessor(
                viewSurgeTrackingReader,
                viewSurgeTrackingWriter,
                pendingViewCountRepository,
                postMetaWriter,
                popularityPromotionInvoker,
                createProperties(),
                new SimpleMeterRegistry()
        );
    }

    @Test
    void 증가량이_있는_게시글만_배치로_반영한다() {
        // given
        when(viewSurgeTrackingReader.readActivePostIds()).thenReturn(List.of(1L, 2L, 3L));
        when(pendingViewCountRepository.getAndResetAll(List.of(1L, 2L, 3L))).thenReturn(List.of(50L, 0L, 30L));

        // when
        viewCountFlushProcessor.flush();

        // then
        verify(postMetaWriter).applyViewCountDeltas(List.of(
                new ViewCountDelta(1L, 50L),
                new ViewCountDelta(3L, 30L)
        ));
        verify(popularityPromotionInvoker).tryEvaluateAll(
                List.of(1L, 3L),
                PopularityTrigger.VIEW_WRITE_BACK
        );
    }

    @Test
    void 플러시_증가량이_유지_기준을_넘는_게시글만_묶어서_연장한다() {
        // given
        when(viewSurgeTrackingReader.readActivePostIds()).thenReturn(List.of(1L, 2L));
        when(pendingViewCountRepository.getAndResetAll(List.of(1L, 2L)))
                .thenReturn(List.of(SUSTAIN_THRESHOLD, SUSTAIN_THRESHOLD - 1));
        when(viewSurgeTrackingWriter.extendAll(List.of(1L))).thenReturn(1);

        // when
        viewCountFlushProcessor.flush();

        // then
        verify(viewSurgeTrackingWriter).extendAll(List.of(1L));
    }

    @Test
    void 반영_실패가_확실하면_증가량을_버퍼에_되돌린다() {
        // given
        when(viewSurgeTrackingReader.readActivePostIds()).thenReturn(List.of(1L));
        when(pendingViewCountRepository.getAndResetAll(List.of(1L))).thenReturn(List.of(50L));
        doThrow(new DataIntegrityViolationException("확실한 실패"))
                .when(postMetaWriter).applyViewCountDeltas(any());

        // when // then
        assertThatThrownBy(() -> viewCountFlushProcessor.flush())
                .isInstanceOf(DataIntegrityViolationException.class);
        verify(pendingViewCountRepository).restore(1L, 50L);
    }

    @Test
    void 커밋_여부를_알_수_없는_실패에서는_복구하지_않는다() {
        // given — 복구하면 같은 증가량이 두 번 반영될 수 있다
        when(viewSurgeTrackingReader.readActivePostIds()).thenReturn(List.of(1L));
        when(pendingViewCountRepository.getAndResetAll(List.of(1L))).thenReturn(List.of(50L));
        doThrow(new QueryTimeoutException("커밋 여부 불명"))
                .when(postMetaWriter).applyViewCountDeltas(any());

        // when // then
        assertThatThrownBy(() -> viewCountFlushProcessor.flush())
                .isInstanceOf(QueryTimeoutException.class);
        verify(pendingViewCountRepository, never()).restore(anyLong(), anyLong());
    }

    @Test
    void 파이프라인이_실패하면_이번_주기를_조기_종료한다() {
        // given
        when(viewSurgeTrackingReader.readActivePostIds()).thenReturn(List.of(1L, 2L));
        when(pendingViewCountRepository.getAndResetAll(List.of(1L, 2L)))
                .thenThrow(new RedisConnectionFailureException("연결 실패"));

        // when
        viewCountFlushProcessor.flush();

        // then
        verify(postMetaWriter, never()).applyViewCountDeltas(any());
    }

    private ViewSurgeProperties createProperties() {
        return new ViewSurgeProperties(
                Duration.ofSeconds(10),
                200L,
                SUSTAIN_THRESHOLD,
                Duration.ofMinutes(5),
                Duration.ofMinutes(5),
                Duration.ofSeconds(15),
                1_000,
                Duration.ofSeconds(60),
                1_000,
                100
        );
    }
}
