package cluverse.popularity.service.implement;

import cluverse.popularity.domain.PopularityAlgorithmVersion;
import cluverse.popularity.domain.PopularityTrigger;
import cluverse.popularity.properties.PopularityProperties;
import cluverse.popularity.repository.dto.PopularitySnapshot;
import org.junit.jupiter.api.BeforeEach;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PopularityPromotionProcessorV2Test {

    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Seoul");
    private static final Instant NOW = Instant.parse("2026-08-01T03:00:00Z");
    private static final LocalDateTime NOW_LOCAL = LocalDateTime.ofInstant(NOW, ZONE_ID);

    @Mock
    private PopularitySnapshotReader popularitySnapshotReader;

    @Mock
    private PopularityPolicyReader popularityPolicyReader;

    @Mock
    private PopularPostWriter popularPostWriter;

    @Mock
    private PopularityCandidateWriter popularityCandidateWriter;

    @Mock
    private PopularityMetricsRecorder popularityMetricsRecorder;

    private PopularityPromotionProcessorV2 processor;

    @BeforeEach
    void setUp() {
        PopularityProperties properties = new PopularityProperties(
                100L,
                5,
                3,
                3,
                2,
                1,
                Duration.ofHours(48),
                Duration.ofDays(7),
                0.98,
                100,
                0.30,
                Duration.ofMinutes(1),
                Duration.ofMinutes(1),
                1_000,
                Duration.ofSeconds(30),
                500,
                Duration.ofSeconds(30),
                500,
                false,
                ""
        );
        processor = new PopularityPromotionProcessorV2(
                popularitySnapshotReader,
                popularityPolicyReader,
                popularPostWriter,
                popularityCandidateWriter,
                popularityMetricsRecorder,
                properties,
                Clock.fixed(NOW, ZONE_ID)
        );
    }

    @Test
    void 반응_게이트를_통과했지만_점수가_부족하면_재검사_후보로_남긴다() {
        // given
        PopularitySnapshot snapshot = snapshot(5, 0, 10);
        when(popularitySnapshotReader.read(1L)).thenReturn(snapshot);
        when(popularityPolicyReader.read(10L)).thenReturn(policy(100, 5, 3));

        // when
        processor.evaluate(1L, PopularityTrigger.LIKE);

        // then
        verify(popularityCandidateWriter).upsert(
                1L,
                10L,
                NOW_LOCAL,
                NOW_LOCAL.plusSeconds(30),
                snapshot.createdAt().plusHours(48)
        );
        verify(popularPostWriter, never()).promote(any(), any(), any(), any());
    }

    @Test
    void 후보_등록_뒤_완만한_조회수_증가로_점수를_넘으면_재검사에서_승격한다() {
        // given
        PopularitySnapshot snapshot = snapshot(5, 0, 90);
        when(popularitySnapshotReader.read(1L)).thenReturn(snapshot);
        when(popularityPolicyReader.read(10L)).thenReturn(policy(100, 5, 3));

        // when
        processor.evaluate(1L, PopularityTrigger.CANDIDATE_RECHECK);

        // then
        InOrder inOrder = inOrder(popularPostWriter, popularityCandidateWriter);
        inOrder.verify(popularPostWriter).promote(
                PopularityAlgorithmVersion.V2,
                snapshot,
                policy(100, 5, 3),
                PopularityTrigger.CANDIDATE_RECHECK
        );
        inOrder.verify(popularityCandidateWriter).remove(1L);
    }

    @Test
    void 게시판별_정책의_반응_게이트를_통과하지_못하면_후보로_등록하지_않는다() {
        // given
        when(popularitySnapshotReader.read(1L)).thenReturn(snapshot(4, 2, 1_000));
        when(popularityPolicyReader.read(10L)).thenReturn(policy(100, 5, 3));

        // when
        processor.evaluate(1L, PopularityTrigger.LIKE);

        // then
        verify(popularityCandidateWriter, never()).upsert(any(), any(), any(), any(), any());
        verify(popularPostWriter, never()).promote(any(), any(), any(), any());
    }

    @Test
    void 작성_후_48시간이_지난_글은_후보에서_제거하고_승격하지_않는다() {
        // given
        PopularitySnapshot expired = new PopularitySnapshot(
                1L,
                10L,
                NOW_LOCAL.minusHours(48),
                10,
                10,
                1_000
        );
        when(popularitySnapshotReader.read(1L)).thenReturn(expired);

        // when
        processor.evaluate(1L, PopularityTrigger.CANDIDATE_RECHECK);

        // then
        verify(popularityCandidateWriter).remove(1L);
        verify(popularPostWriter, never()).promote(any(), any(), any(), any());
    }

    private PopularitySnapshot snapshot(long likeCount, long commentCount, long viewCount) {
        return new PopularitySnapshot(
                1L,
                10L,
                NOW_LOCAL.minusHours(1),
                likeCount,
                commentCount,
                viewCount
        );
    }

    private PopularityPolicy policy(long promotionScore, int likeGate, int commentGate) {
        return new PopularityPolicy(promotionScore, likeGate, commentGate);
    }
}
