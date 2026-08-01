package cluverse.popularity.service.implement;

import cluverse.meta.repository.PendingViewCountRepository;
import cluverse.meta.service.implement.PostMetaWriter;
import cluverse.popularity.domain.PopularPost;
import cluverse.popularity.domain.PopularityAlgorithmVersion;
import cluverse.popularity.properties.PopularityProperties;
import cluverse.popularity.repository.PopularPostRepository;
import cluverse.popularity.repository.dto.PopularitySnapshot;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.QueryTimeoutException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @Test
    void 다른_워커가_claim한_게시글은_최종화하지_않는다() {
        // given
        ZoneId zoneId = ZoneId.of("Asia/Seoul");
        Instant now = Instant.parse("2026-08-01T03:00:00Z");
        LocalDateTime nowLocal = LocalDateTime.ofInstant(now, zoneId);
        PopularPost popularPost = mock(PopularPost.class);
        when(popularPost.getPostId()).thenReturn(1L);
        when(popularPostRepository.findDuePostIdsForFinalization(nowLocal, 500))
                .thenReturn(List.of(1L));
        when(popularPostRepository.findDueForFinalization(List.of(1L), nowLocal))
                .thenReturn(List.of(popularPost));
        when(popularityFinalizationClaimWriter.claim(eq(1L), anyString(), eq(nowLocal), eq(nowLocal.minusSeconds(90))))
                .thenReturn(false);
        PopularityFinalizationProcessor processor = processor(Clock.fixed(now, zoneId), new SimpleMeterRegistry());

        // when
        int finalized = processor.finalizeDue();

        // then
        assertThat(finalized).isZero();
        verify(pendingViewCountRepository, never()).getAndReset(anyLong());
        verify(popularPostWriter, never()).finalizeSnapshot(anyLong(), any(), any());
    }

    @Test
    void claim_시각은_각_게시글을_처리하는_시점에_새로_계산한다() {
        // given
        ZoneId zoneId = ZoneId.of("Asia/Seoul");
        Instant batchStartedAt = Instant.parse("2026-08-01T03:00:00Z");
        Instant firstClaimedAt = batchStartedAt.plusSeconds(10);
        Instant secondClaimedAt = batchStartedAt.plusSeconds(20);
        LocalDateTime batchNow = LocalDateTime.ofInstant(batchStartedAt, zoneId);
        LocalDateTime firstClaimNow = LocalDateTime.ofInstant(firstClaimedAt, zoneId);
        LocalDateTime secondClaimNow = LocalDateTime.ofInstant(secondClaimedAt, zoneId);
        Clock advancingClock = mock(Clock.class);
        when(advancingClock.instant()).thenReturn(batchStartedAt, firstClaimedAt, secondClaimedAt);
        when(advancingClock.getZone()).thenReturn(zoneId);
        PopularPost first = mock(PopularPost.class);
        PopularPost second = mock(PopularPost.class);
        when(first.getPostId()).thenReturn(1L);
        when(second.getPostId()).thenReturn(2L);
        when(popularPostRepository.findDuePostIdsForFinalization(batchNow, 500))
                .thenReturn(List.of(1L, 2L));
        when(popularPostRepository.findDueForFinalization(List.of(1L, 2L), batchNow))
                .thenReturn(List.of(first, second));
        PopularityFinalizationProcessor processor = processor(advancingClock, new SimpleMeterRegistry());

        // when
        processor.finalizeDue();

        // then
        verify(popularityFinalizationClaimWriter).claim(
                eq(1L), anyString(), eq(firstClaimNow), eq(firstClaimNow.minusSeconds(90))
        );
        verify(popularityFinalizationClaimWriter).claim(
                eq(2L), anyString(), eq(secondClaimNow), eq(secondClaimNow.minusSeconds(90))
        );
    }

    @Test
    void 커밋_여부가_불확실한_pending_반영_실패를_지표로_남긴다() {
        // given
        ZoneId zoneId = ZoneId.of("Asia/Seoul");
        Instant now = Instant.parse("2026-08-01T03:00:00Z");
        LocalDateTime nowLocal = LocalDateTime.ofInstant(now, zoneId);
        PopularPost popularPost = mock(PopularPost.class);
        when(popularPost.getPostId()).thenReturn(1L);
        when(popularPostRepository.findDuePostIdsForFinalization(nowLocal, 500))
                .thenReturn(List.of(1L));
        when(popularPostRepository.findDueForFinalization(List.of(1L), nowLocal))
                .thenReturn(List.of(popularPost));
        when(popularityFinalizationClaimWriter.claim(eq(1L), anyString(), eq(nowLocal), eq(nowLocal.minusSeconds(90))))
                .thenReturn(true);
        when(pendingViewCountRepository.getAndReset(1L)).thenReturn(7L);
        doThrow(new QueryTimeoutException("커밋 여부 불명"))
                .when(postMetaWriter).applyViewCountDeltas(any());
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        PopularityFinalizationProcessor processor = processor(Clock.fixed(now, zoneId), meterRegistry);

        // when
        processor.finalizeDue();

        // then
        assertThat(meterRegistry.get("popularity.finalization.pending.loss.risk")
                .tag("reason", "ROLLBACK_UNCERTAIN")
                .counter()
                .count()).isEqualTo(1.0);
        verify(pendingViewCountRepository, never()).restore(anyLong(), anyLong());
    }

    private PopularityFinalizationProcessor processor(Clock clock, SimpleMeterRegistry meterRegistry) {
        return new PopularityFinalizationProcessor(
                popularPostRepository,
                pendingViewCountRepository,
                postMetaWriter,
                popularitySnapshotReader,
                popularPostWriter,
                popularityFinalizationClaimWriter,
                properties(),
                clock,
                meterRegistry
        );
    }

    private List<cluverse.meta.repository.dto.ViewCountDelta> anyDelta(Long postId, long delta) {
        return List.of(new cluverse.meta.repository.dto.ViewCountDelta(postId, delta));
    }

    private PopularityProperties properties() {
        return new PopularityProperties(
                100L, 5, 3, 3, 2, 1,
                Duration.ofHours(48), Duration.ofDays(7), 0.98, 100, 0.30,
                Duration.ofMinutes(1), Duration.ofMinutes(1), false, 1_000,
                Duration.ofSeconds(30), 500,
                Duration.ofSeconds(30), 500,
                false, ""
        );
    }
}
