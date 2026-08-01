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

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PopularityPolicyBoardRefreshProcessorTest {

    private static final LocalDateTime COMPUTED_AT = LocalDateTime.of(2026, 8, 1, 12, 0);
    private static final LocalDateTime SAMPLE_END = COMPUTED_AT.minusHours(48);
    private static final LocalDateTime SAMPLE_START = SAMPLE_END.minusDays(7);

    @Mock
    private PopularityQueryRepository popularityQueryRepository;

    @Mock
    private BoardPopularityPolicyRepository boardPopularityPolicyRepository;

    @Mock
    private PopularityPolicyCache popularityPolicyCache;

    @Test
    void 표본이_부족하면_게시판에_기본_정책을_저장한다() {
        // given
        PopularityProperties properties = properties(3, 0.5, 0.5);
        when(popularityQueryRepository.findPolicySamples(10L, SAMPLE_START, SAMPLE_END))
                .thenReturn(List.of(sample(null, 1, 0, 10), sample(null, 2, 0, 20)));
        when(boardPopularityPolicyRepository.findById(10L)).thenReturn(Optional.empty());
        PopularityPolicyBoardRefreshProcessor processor = processor(properties);

        // when
        processor.refreshBoard(10L, SAMPLE_START, SAMPLE_END, COMPUTED_AT);

        // then
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
    void 최소_표본_설정이_0이어도_빈_표본은_기본_정책을_사용한다() {
        // given
        PopularityProperties properties = properties(0, 0.5, 0.5);
        when(popularityQueryRepository.findPolicySamples(10L, SAMPLE_START, SAMPLE_END))
                .thenReturn(List.of());
        when(boardPopularityPolicyRepository.findById(10L)).thenReturn(Optional.empty());
        PopularityPolicyBoardRefreshProcessor processor = processor(properties);

        // when
        processor.refreshBoard(10L, SAMPLE_START, SAMPLE_END, COMPUTED_AT);

        // then
        ArgumentCaptor<BoardPopularityPolicy> captor = ArgumentCaptor.forClass(BoardPopularityPolicy.class);
        verify(boardPopularityPolicyRepository).save(captor.capture());
        assertThat(captor.getValue().getPolicySource()).isEqualTo(PopularityPolicySource.DEFAULT);
        assertThat(captor.getValue().getPromotionScore()).isEqualTo(100);
    }

    @Test
    void 승격_시점_점수의_백분위와_기존_정책을_스무딩한다() {
        // given
        PopularityProperties properties = properties(3, 0.5, 0.5);
        BoardPopularityPolicy existing = BoardPopularityPolicy.create(
                10L, 30, 3, 1, 10, PopularityPolicySource.DISTRIBUTION, SAMPLE_START
        );
        when(popularityQueryRepository.findPolicySamples(10L, SAMPLE_START, SAMPLE_END)).thenReturn(List.of(
                sample(10L, 1, 0, 1_000),
                sample(50L, 5, 2, 1_000),
                sample(100L, 10, 4, 1_000)
        ));
        when(boardPopularityPolicyRepository.findById(10L)).thenReturn(Optional.of(existing));
        PopularityPolicyBoardRefreshProcessor processor = processor(properties);

        // when
        processor.refreshBoard(10L, SAMPLE_START, SAMPLE_END, COMPUTED_AT);

        // then
        assertThat(existing.getPromotionScore()).isEqualTo(40);
        assertThat(existing.getLikeGate()).isEqualTo(4);
        assertThat(existing.getCommentGate()).isEqualTo(2);
        assertThat(existing.getSampleSize()).isEqualTo(3);
        assertThat(existing.getPolicySource()).isEqualTo(PopularityPolicySource.DISTRIBUTION);
        verify(boardPopularityPolicyRepository).save(existing);
        verify(popularityPolicyCache).put(10L, new PopularityPolicy(40, 4, 2));
    }

    private PopularityPolicyBoardRefreshProcessor processor(PopularityProperties properties) {
        return new PopularityPolicyBoardRefreshProcessor(
                popularityQueryRepository,
                boardPopularityPolicyRepository,
                popularityPolicyCache,
                properties
        );
    }

    private PopularityPolicySample sample(Long scoreAtPromotion, long likes, long comments, long views) {
        return new PopularityPolicySample(scoreAtPromotion, likes, comments, views);
    }

    private PopularityProperties properties(int minSampleSize, double percentile, double smoothingRatio) {
        return new PopularityProperties(
                100L, 5, 3, 3, 2, 1,
                Duration.ofHours(48), Duration.ofDays(7), percentile, minSampleSize, smoothingRatio,
                Duration.ofMinutes(1), Duration.ofMinutes(1), false, 1_000,
                Duration.ofSeconds(30), 500,
                Duration.ofSeconds(30), 500,
                false, ""
        );
    }
}
