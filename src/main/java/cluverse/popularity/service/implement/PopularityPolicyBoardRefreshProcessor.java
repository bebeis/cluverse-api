package cluverse.popularity.service.implement;

import cluverse.popularity.domain.BoardPopularityPolicy;
import cluverse.popularity.domain.PopularityPolicySource;
import cluverse.popularity.properties.PopularityProperties;
import cluverse.popularity.repository.BoardPopularityPolicyRepository;
import cluverse.popularity.repository.PopularityQueryRepository;
import cluverse.popularity.repository.dto.PopularityPolicySample;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PopularityPolicyBoardRefreshProcessor {

    private final PopularityQueryRepository popularityQueryRepository;
    private final BoardPopularityPolicyRepository boardPopularityPolicyRepository;
    private final PopularityPolicyCache popularityPolicyCache;
    private final PopularityProperties properties;

    @Transactional
    public void refreshBoard(
            Long boardId,
            LocalDateTime sampleStart,
            LocalDateTime sampleEnd,
            LocalDateTime computedAt
    ) {
        List<PopularityPolicySample> samples = popularityQueryRepository.findPolicySamples(
                boardId,
                sampleStart,
                sampleEnd
        );
        boolean enoughSamples = hasEnoughSamples(samples);
        PopularityPolicy calculated = enoughSamples ? percentilePolicy(samples) : defaultPolicy();
        Optional<BoardPopularityPolicy> existingPolicy = boardPopularityPolicyRepository.findById(boardId);
        PopularityPolicy smoothed = existingPolicy
                .map(existing -> smooth(existing, calculated))
                .orElse(calculated);
        PopularityPolicySource source = enoughSamples
                ? PopularityPolicySource.DISTRIBUTION
                : PopularityPolicySource.DEFAULT;

        BoardPopularityPolicy entity = existingPolicy
                .orElseGet(() -> BoardPopularityPolicy.create(
                        boardId,
                        smoothed.promotionScore(),
                        smoothed.likeGate(),
                        smoothed.commentGate(),
                        samples.size(),
                        source,
                        computedAt
                ));
        entity.replace(
                smoothed.promotionScore(),
                smoothed.likeGate(),
                smoothed.commentGate(),
                samples.size(),
                source,
                computedAt
        );
        boardPopularityPolicyRepository.save(entity);
        popularityPolicyCache.put(boardId, smoothed);
    }

    private boolean hasEnoughSamples(List<PopularityPolicySample> samples) {
        return !samples.isEmpty() && samples.size() >= properties.policyMinSampleSize();
    }

    private PopularityPolicy percentilePolicy(List<PopularityPolicySample> samples) {
        return new PopularityPolicy(
                percentile(samples.stream().map(this::resolveSampleScore).toList()),
                Math.toIntExact(percentile(samples.stream().map(PopularityPolicySample::likeCount).toList())),
                Math.toIntExact(percentile(samples.stream().map(PopularityPolicySample::commentCount).toList()))
        );
    }

    private long resolveSampleScore(PopularityPolicySample sample) {
        if (sample.scoreAtPromotion() != null) {
            return sample.scoreAtPromotion();
        }
        return sample.likeCount() * properties.scoreLikeWeight()
                + sample.commentCount() * properties.scoreCommentWeight()
                + sample.viewCount() * properties.scoreViewWeight();
    }

    private long percentile(List<Long> values) {
        if (values.isEmpty()) {
            throw new IllegalArgumentException("백분위 계산에는 표본이 필요합니다.");
        }
        List<Long> sorted = values.stream().sorted(Comparator.naturalOrder()).toList();
        int index = Math.max(0, (int) Math.ceil(properties.policyPercentile() * sorted.size()) - 1);
        return sorted.get(index);
    }

    private PopularityPolicy smooth(BoardPopularityPolicy old, PopularityPolicy calculated) {
        double ratio = properties.policySmoothingRatio();
        return new PopularityPolicy(
                Math.round(old.getPromotionScore() * (1 - ratio) + calculated.promotionScore() * ratio),
                (int) Math.round(old.getLikeGate() * (1 - ratio) + calculated.likeGate() * ratio),
                (int) Math.round(old.getCommentGate() * (1 - ratio) + calculated.commentGate() * ratio)
        );
    }

    private PopularityPolicy defaultPolicy() {
        return new PopularityPolicy(
                properties.defaultPromotionScore(),
                properties.defaultLikeGate(),
                properties.defaultCommentGate()
        );
    }
}
