package cluverse.popularity.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "popularity")
public record PopularityProperties(
        long defaultPromotionScore,
        int defaultLikeGate,
        int defaultCommentGate,
        int scoreLikeWeight,
        int scoreCommentWeight,
        int scoreViewWeight,
        Duration promotionWindow,
        Duration policySampleWindow,
        double policyPercentile,
        int policyMinSampleSize,
        double policySmoothingRatio,
        Duration policyCacheRefreshInterval,
        Duration v1ScanInterval,
        int scanChunkSize,
        Duration candidateRecheckInterval,
        int candidateBatchSize,
        Duration finalizationInterval,
        int finalizationBatchSize,
        boolean experimentEndpointsEnabled,
        String benchmarkToken
) {
}
