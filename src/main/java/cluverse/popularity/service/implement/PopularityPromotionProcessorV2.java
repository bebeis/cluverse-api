package cluverse.popularity.service.implement;

import cluverse.popularity.domain.PopularityAlgorithmVersion;
import cluverse.popularity.domain.PopularityTrigger;
import cluverse.popularity.properties.PopularityProperties;
import cluverse.popularity.repository.dto.PopularitySnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional
public class PopularityPromotionProcessorV2 {

    private final PopularitySnapshotReader popularitySnapshotReader;
    private final PopularityPolicyReader popularityPolicyReader;
    private final PopularPostWriter popularPostWriter;
    private final PopularityMetricsRecorder popularityMetricsRecorder;
    private final PopularityProperties properties;
    private final Clock clock;

    public void evaluateAll(List<Long> postIds, PopularityTrigger trigger) {
        List<PopularitySnapshot> snapshots = popularitySnapshotReader.readAll(postIds);
        for (PopularitySnapshot snapshot : snapshots) {
            evaluateSnapshot(PopularityAlgorithmVersion.V2, snapshot, trigger);
        }
    }

    public void evaluate(Long postId, PopularityTrigger trigger) {
        PopularitySnapshot snapshot = popularitySnapshotReader.read(postId);
        if (snapshot == null) {
            popularityMetricsRecorder.evaluated(PopularityAlgorithmVersion.V2, trigger, "NOT_FOUND");
            return;
        }
        evaluateSnapshot(PopularityAlgorithmVersion.V2, snapshot, trigger);
    }

    public void evaluateBaseline(PopularitySnapshot snapshot) {
        evaluateSnapshot(PopularityAlgorithmVersion.V1, snapshot, PopularityTrigger.PERIODIC_SCAN);
    }

    private void evaluateSnapshot(
            PopularityAlgorithmVersion version,
            PopularitySnapshot snapshot,
            PopularityTrigger trigger
    ) {
        long startedAt = System.nanoTime();
        try {
            evaluateInternal(version, snapshot, trigger);
        } finally {
            popularityMetricsRecorder.recordEvaluationDuration(
                    version,
                    trigger,
                    System.nanoTime() - startedAt
            );
        }
    }

    private void evaluateInternal(
            PopularityAlgorithmVersion version,
            PopularitySnapshot snapshot,
            PopularityTrigger trigger
    ) {
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), clock.getZone());
        LocalDateTime expiresAt = snapshot.createdAt().plus(properties.promotionWindow());
        popularityMetricsRecorder.examined(version, 1);

        if (!now.isBefore(expiresAt)) {
            popularityMetricsRecorder.evaluated(version, trigger, "EXPIRED");
            return;
        }

        PopularityPolicy policy = version == PopularityAlgorithmVersion.V1
                ? new PopularityPolicy(properties.defaultPromotionScore())
                : popularityPolicyReader.read(snapshot.boardId());

        long score = calculateScore(snapshot);
        if (score >= policy.promotionScore()) {
            popularPostWriter.promote(version, snapshot, policy, trigger);
            popularityMetricsRecorder.evaluated(version, trigger, "PROMOTED");
            popularityMetricsRecorder.promoted(version, trigger);
            return;
        }
        popularityMetricsRecorder.evaluated(version, trigger, "BELOW_THRESHOLD");
    }

    private long calculateScore(PopularitySnapshot snapshot) {
        return scorePolicy().calculate(snapshot.likeCount(), snapshot.commentCount());
    }

    private PopularityScore scorePolicy() {
        return new PopularityScore(properties.scoreLikeWeight(), properties.scoreCommentWeight());
    }
}
