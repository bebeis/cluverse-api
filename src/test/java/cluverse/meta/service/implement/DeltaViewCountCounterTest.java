package cluverse.meta.service.implement;

import cluverse.meta.properties.ViewCountProperties;
import cluverse.meta.repository.DeltaViewCountRepository;
import cluverse.meta.repository.DeltaViewCountVersion;
import cluverse.meta.repository.dto.DeltaViewCountResult;
import cluverse.meta.repository.dto.ViewCountDelta;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeltaViewCountCounterTest {

    @Mock
    private DeltaViewCountRepository deltaViewCountRepository;

    @Mock
    private PostMetaReader postMetaReader;

    @Mock
    private PostMetaWriter postMetaWriter;

    @Mock
    private ViewCountProperties properties;

    @InjectMocks
    private DeltaViewCountCounter deltaViewCountCounter;

    @Test
    void 시간_기반_방식은_DB_조회수와_Redis_증분을_합산한다() {
        when(deltaViewCountRepository.count(DeltaViewCountVersion.TIME_BASED, 10L, "viewer-1"))
                .thenReturn(new DeltaViewCountResult(true, 25L));
        when(postMetaReader.readViewCount(10L)).thenReturn(1_000L);

        ViewCountResult result = deltaViewCountCounter.countTimeBased(10L, "viewer-1");

        assertThat(result).isEqualTo(new ViewCountResult(1_025L, true, ViewCountSource.REDIS_DELTA));
    }

    @Test
    void 기준치에_도달하면_증분을_DB로_반영한다() {
        when(properties.threshold()).thenReturn(100L);
        when(deltaViewCountRepository.count(DeltaViewCountVersion.THRESHOLD, 10L, "viewer-1"))
                .thenReturn(new DeltaViewCountResult(true, 100L));
        when(deltaViewCountRepository.take(DeltaViewCountVersion.THRESHOLD, 10L)).thenReturn(100L);
        when(postMetaReader.readViewCount(10L)).thenReturn(1_100L);

        ViewCountResult result = deltaViewCountCounter.countThreshold(10L, "viewer-1");

        verify(postMetaWriter).applyViewCountDeltas(List.of(new ViewCountDelta(10L, 100L)));
        assertThat(result.viewCount()).isEqualTo(1_100L);
    }

    @Test
    void 시간_기반_flush가_실패하면_꺼낸_증분을_Redis에_복원한다() {
        when(deltaViewCountRepository.findPostIds(DeltaViewCountVersion.TIME_BASED)).thenReturn(List.of(10L));
        when(deltaViewCountRepository.take(DeltaViewCountVersion.TIME_BASED, 10L)).thenReturn(50L);
        doThrow(new IllegalStateException("db unavailable"))
                .when(postMetaWriter).applyViewCountDeltas(List.of(new ViewCountDelta(10L, 50L)));

        assertThatThrownBy(deltaViewCountCounter::flushTimeBased)
                .isInstanceOf(IllegalStateException.class);
        verify(deltaViewCountRepository).restore(DeltaViewCountVersion.TIME_BASED, 10L, 50L);
    }
}
