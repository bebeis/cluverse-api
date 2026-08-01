package cluverse.popularity.service.implement;

import cluverse.meta.repository.PendingViewCountRepository;
import cluverse.meta.service.implement.PostMetaWriter;
import cluverse.popularity.domain.PopularPost;
import cluverse.popularity.domain.PopularityAlgorithmVersion;
import cluverse.popularity.properties.PopularityProperties;
import cluverse.popularity.repository.PopularPostRepository;
import cluverse.popularity.repository.dto.PopularitySnapshot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@ExtendWith(MockitoExtension.class)
class PopularityFinalizationProcessorTest {

    @Mock
    private PopularPostRepository popularPostRepository;

    @Mock
    private PendingViewCountRepository pendingViewCountRepository;

    @Mock
    private PostMetaWriter postMetaWriter;

    @Mock
    private PopularitySnapshotReader popularitySnapshotReader;

    @Mock
    private PopularPostWriter popularPostWriter;

    @Mock
    private PopularityFinalizationClaimWriter popularityFinalizationClaimWriter;

    @Test
    void 미반영_조회수를_DB에_플러시한_뒤_최종_점수_스냅샷을_읽는다() {
        // given
        ZoneId zoneId = ZoneId.of("Asia/Seoul");
        Instant now = Instant.parse("2026-08-01T03:00:00Z");
        LocalDateTime nowLocal = LocalDateTime.ofInstant(now, zoneId);
        PopularPost popularPost = mock(PopularPost.class);
        when(popularPost.getId()).thenReturn(100L);
        when(popularPost.getPostId()).thenReturn(1L);
        when(popularPost.getAlgorithmVersion()).thenReturn(PopularityAlgorithmVersion.V2);
        when(popularPost.getFinalizeAt()).thenReturn(nowLocal);
        when(popularPostRepository.findDuePostIdsForFinalization(nowLocal, 500))
                .thenReturn(List.of(1L));
        when(popularPostRepository.findDueForFinalization(List.of(1L), nowLocal))
                .thenReturn(List.of(popularPost));
        when(popularityFinalizationClaimWriter.claim(
                org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.eq(nowLocal),
                org.mockito.ArgumentMatchers.eq(nowLocal.minusSeconds(90))
        )).thenReturn(true);
        when(pendingViewCountRepository.getAndReset(1L)).thenReturn(7L);
        PopularitySnapshot snapshot = new PopularitySnapshot(
                1L, 10L, nowLocal.minusHours(48), 10, 5, 100
        );
        when(popularitySnapshotReader.read(1L)).thenReturn(snapshot);
        when(popularPostWriter.finalizeSnapshot(100L, snapshot, nowLocal)).thenReturn(true);
        PopularityFinalizationProcessor processor = new PopularityFinalizationProcessor(
                popularPostRepository,
                pendingViewCountRepository,
                postMetaWriter,
                popularitySnapshotReader,
                popularPostWriter,
                popularityFinalizationClaimWriter,
                properties(),
                Clock.fixed(now, zoneId),
                new SimpleMeterRegistry()
        );

        // when
        processor.finalizeDue();

        // then
        InOrder inOrder = inOrder(
                pendingViewCountRepository,
                postMetaWriter,
                popularitySnapshotReader,
                popularPostWriter
        );
        inOrder.verify(pendingViewCountRepository).getAndReset(1L);
        inOrder.verify(postMetaWriter).applyViewCountDeltas(anyDelta(1L, 7L));
        inOrder.verify(popularitySnapshotReader).read(1L);
        inOrder.verify(popularPostWriter).finalizeSnapshot(100L, snapshot, nowLocal);
    }

    private List<cluverse.meta.repository.dto.ViewCountDelta> anyDelta(Long postId, long delta) {
        return List.of(new cluverse.meta.repository.dto.ViewCountDelta(postId, delta));
    }

    private PopularityProperties properties() {
        return new PopularityProperties(
                100L, 5, 3, 3, 2, 1,
                Duration.ofHours(48), Duration.ofDays(7), 0.98, 100, 0.30,
                Duration.ofMinutes(1), Duration.ofMinutes(1), 1_000,
                Duration.ofSeconds(30), 500,
                Duration.ofSeconds(30), 500,
                false, ""
        );
    }
}
