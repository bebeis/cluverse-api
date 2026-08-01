package cluverse.popularity.service.implement;

import cluverse.popularity.domain.BoardPopularityPolicy;
import cluverse.popularity.domain.PopularityPolicySource;
import cluverse.popularity.properties.PopularityProperties;
import cluverse.popularity.repository.BoardPopularityPolicyRepository;
import cluverse.popularity.repository.PopularityQueryRepository;
import cluverse.popularity.repository.dto.PopularityPolicySample;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PopularityPolicyRefreshProcessorTest {

    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Seoul");
    private static final Instant NOW = Instant.parse("2026-08-01T03:00:00Z");
    private static final LocalDateTime NOW_LOCAL = LocalDateTime.ofInstant(NOW, ZONE_ID);

    @Mock
    private PopularityQueryRepository popularityQueryRepository;

    @Mock
    private BoardPopularityPolicyRepository boardPopularityPolicyRepository;

    @Mock
    private PopularityPolicyCache popularityPolicyCache;

    @Test
    void 표본이_부족하면_게시판에_기본_정책을_저장한다() {
        PopularityProperties properties = properties(3, 0.5, 0.5);
        LocalDateTime sampleEnd = NOW_LOCAL.minusHours(48);
        LocalDateTime sampleStart = sampleEnd.minusDays(7);
        when(popularityQueryRepository.findPolicyBoardIds(sampleStart, sampleEnd)).thenReturn(List.of(10L));
        when(popularityQueryRepository.findPolicySamples(10L, sampleStart, sampleEnd))
                .thenReturn(List.of(sample(null, 1, 0, 10), sample(null, 2, 0, 20)));
        when(boardPopularityPolicyRepository.findById(10L)).thenReturn(Optional.empty());
        PopularityPolicyRefreshProcessor processor = processor(properties);

        assertThat(processor.refresh()).isEqualTo(1);

        ArgumentCaptor<BoardPopularityPolicy> captor = ArgumentCaptor.forClass(BoardPopularityPolicy.class);
        verify(boardPopularityPolicyRepository).save(captor.capture());
        BoardPopularityPolicy saved = captor.getValue();
        assertThat(saved.getPromotionScore()).isEqualTo(100);
        assertThat(saved.getLikeGate()).isEqualTo(5);
        assertThat(saved.getCommentGate()).isEqualTo(3);
        assertThat(saved.getSampleSize()).isEqualTo(2);
        assertThat(saved.getPolicySource()).isEqualTo(PopularityPolicySource.DEFAULT);
        verify(popularityPolicyCache).put(10L, new PopularityPolicy(100, 5, 3));
    }

    @Test
    void 승격_시점_점수의_백분위와_기존_정책을_스무딩한다() {
        PopularityProperties properties = properties(3, 0.5, 0.5);
        LocalDateTime sampleEnd = NOW_LOCAL.minusHours(48);
        LocalDateTime sampleStart = sampleEnd.minusDays(7);
        BoardPopularityPolicy existing = BoardPopularityPolicy.create(
                10L, 30, 3, 1, 10, PopularityPolicySource.DISTRIBUTION, sampleStart
        );
        when(popularityQueryRepository.findPolicyBoardIds(sampleStart, sampleEnd)).thenReturn(List.of(10L));
        when(popularityQueryRepository.findPolicySamples(10L, sampleStart, sampleEnd)).thenReturn(List.of(
                sample(10L, 1, 0, 1_000),
                sample(50L, 5, 2, 1_000),
                sample(100L, 10, 4, 1_000)
        ));
        when(boardPopularityPolicyRepository.findById(10L)).thenReturn(Optional.of(existing));
        PopularityPolicyRefreshProcessor processor = processor(properties);

        processor.refresh();

        assertThat(existing.getPromotionScore()).isEqualTo(40);
        assertThat(existing.getLikeGate()).isEqualTo(4);
        assertThat(existing.getCommentGate()).isEqualTo(2);
        assertThat(existing.getSampleSize()).isEqualTo(3);
        assertThat(existing.getPolicySource()).isEqualTo(PopularityPolicySource.DISTRIBUTION);
        verify(boardPopularityPolicyRepository).save(existing);
        verify(popularityPolicyCache).put(10L, new PopularityPolicy(40, 4, 2));
    }

    private PopularityPolicyRefreshProcessor processor(PopularityProperties properties) {
        return new PopularityPolicyRefreshProcessor(
                popularityQueryRepository,
                boardPopularityPolicyRepository,
                popularityPolicyCache,
                properties,
                Clock.fixed(NOW, ZONE_ID)
        );
    }

    private PopularityPolicySample sample(Long scoreAtPromotion, long likes, long comments, long views) {
        return new PopularityPolicySample(scoreAtPromotion, likes, comments, views);
    }

    private PopularityProperties properties(int minSampleSize, double percentile, double smoothingRatio) {
        return new PopularityProperties(
                100L, 5, 3, 3, 2, 1,
                Duration.ofHours(48), Duration.ofDays(7), percentile, minSampleSize, smoothingRatio,
                Duration.ofMinutes(1), Duration.ofMinutes(1), 1_000,
                Duration.ofSeconds(30), 500,
                Duration.ofSeconds(30), 500,
                false, ""
        );
    }
}
