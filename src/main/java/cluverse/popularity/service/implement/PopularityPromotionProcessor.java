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
public class PopularityPromotionProcessor {

    private static final PopularityAlgorithmVersion CURRENT_VERSION = PopularityAlgorithmVersion.V2;

    private final PopularitySnapshotReader popularitySnapshotReader;
    private final PopularityPolicyReader popularityPolicyReader;
    private final PopularPostWriter popularPostWriter;
    private final PopularityMetricsRecorder popularityMetricsRecorder;
    private final PopularityProperties properties;
    private final Clock clock;

    public void evaluateAll(List<Long> postIds, PopularityTrigger trigger) {
        List<PopularitySnapshot> snapshots = popularitySnapshotReader.readAll(postIds);
        for (PopularitySnapshot snapshot : snapshots) {
            evaluateSnapshot(snapshot, trigger);
        }
    }

    public void evaluate(Long postId, PopularityTrigger trigger) {
        PopularitySnapshot snapshot = popularitySnapshotReader.read(postId);
        if (snapshot == null) {
            popularityMetricsRecorder.evaluated(CURRENT_VERSION, trigger, "NOT_FOUND");
            return;
        }
        evaluateSnapshot(snapshot, trigger);
    }

    private void evaluateSnapshot(
            PopularitySnapshot snapshot,
            PopularityTrigger trigger
    ) {
        long startedAt = System.nanoTime();
        try {
            evaluateInternal(snapshot, trigger);
        } finally {
            popularityMetricsRecorder.recordEvaluationDuration(
                    CURRENT_VERSION,
                    trigger,
                    System.nanoTime() - startedAt
            );
        }
    }

    private void evaluateInternal(
            PopularitySnapshot snapshot,
            PopularityTrigger trigger
    ) {
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), clock.getZone());
        LocalDateTime expiresAt = snapshot.createdAt().plus(properties.promotionWindow());
        popularityMetricsRecorder.examined(CURRENT_VERSION, 1);

        if (!now.isBefore(expiresAt)) {
            popularityMetricsRecorder.evaluated(CURRENT_VERSION, trigger, "EXPIRED");
            return;
        }

        PopularityPolicy policy = popularityPolicyReader.read(snapshot.boardId());

        long score = calculateScore(snapshot);
        if (score >= policy.promotionScore()) {
            popularPostWriter.promote(CURRENT_VERSION, snapshot, policy, trigger);
            popularityMetricsRecorder.evaluated(CURRENT_VERSION, trigger, "PROMOTED");
            popularityMetricsRecorder.promoted(CURRENT_VERSION, trigger);
            return;
        }
        popularityMetricsRecorder.evaluated(CURRENT_VERSION, trigger, "BELOW_THRESHOLD");
    }

    private long calculateScore(PopularitySnapshot snapshot) {
        return scorePolicy().calculate(snapshot.likeCount(), snapshot.commentCount());
    }

    private PopularityScore scorePolicy() {
        return new PopularityScore(properties.scoreLikeWeight(), properties.scoreCommentWeight());
    }
}
