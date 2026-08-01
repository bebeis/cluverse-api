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
    private final PopularityCandidateWriter popularityCandidateWriter;
    private final PopularityMetricsRecorder popularityMetricsRecorder;
    private final PopularityProperties properties;
    private final Clock clock;

    public void evaluateAll(List<Long> postIds, PopularityTrigger trigger) {
        List<PopularitySnapshot> snapshots = popularitySnapshotReader.readAll(postIds);
        for (PopularitySnapshot snapshot : snapshots) {
            evaluateSnapshot(PopularityAlgorithmVersion.V2, snapshot, trigger, true);
        }
    }

    public void evaluate(Long postId, PopularityTrigger trigger) {
        PopularitySnapshot snapshot = popularitySnapshotReader.read(postId);
        if (snapshot == null) {
            popularityCandidateWriter.remove(postId);
            popularityMetricsRecorder.evaluated(PopularityAlgorithmVersion.V2, trigger, "NOT_FOUND");
            return;
        }
        evaluateSnapshot(PopularityAlgorithmVersion.V2, snapshot, trigger, true);
    }

    public void evaluateBaseline(PopularitySnapshot snapshot) {
        evaluateSnapshot(PopularityAlgorithmVersion.V1, snapshot, PopularityTrigger.PERIODIC_SCAN, false);
    }

    private void evaluateSnapshot(
            PopularityAlgorithmVersion version,
            PopularitySnapshot snapshot,
            PopularityTrigger trigger,
            boolean trackCandidate
    ) {
        long startedAt = System.nanoTime();
        try {
            evaluateInternal(version, snapshot, trigger, trackCandidate);
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
            PopularityTrigger trigger,
            boolean trackCandidate
    ) {
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), clock.getZone());
        LocalDateTime expiresAt = snapshot.createdAt().plus(properties.promotionWindow());
        popularityMetricsRecorder.examined(version, 1);

        if (!now.isBefore(expiresAt)) {
            if (trackCandidate) {
                popularityCandidateWriter.remove(snapshot.postId());
            }
            popularityMetricsRecorder.evaluated(version, trigger, "EXPIRED");
            return;
        }

        PopularityPolicy policy = popularityPolicyReader.read(snapshot.boardId());
        boolean gatePassed = snapshot.likeCount() >= policy.likeGate()
                || snapshot.commentCount() >= policy.commentGate();
        if (!gatePassed) {
            popularityMetricsRecorder.evaluated(version, trigger, "GATE_REJECTED");
            return;
        }

        long score = calculateScore(snapshot);
        if (score >= policy.promotionScore()) {
            popularPostWriter.promote(version, snapshot, policy, trigger);
            if (trackCandidate) {
                popularityCandidateWriter.remove(snapshot.postId());
            }
            popularityMetricsRecorder.evaluated(version, trigger, "PROMOTED");
            popularityMetricsRecorder.promoted(version, trigger);
            return;
        }

        if (trackCandidate) {
            LocalDateTime nextCheckAt = now.plus(properties.candidateRecheckInterval());
            if (trigger == PopularityTrigger.CANDIDATE_RECHECK) {
                popularityCandidateWriter.reschedule(
                        snapshot.postId(), snapshot.boardId(), now, nextCheckAt, expiresAt);
            } else {
                popularityCandidateWriter.upsert(
                        snapshot.postId(), snapshot.boardId(), now, nextCheckAt, expiresAt);
            }
        }
        popularityMetricsRecorder.evaluated(version, trigger, "CANDIDATE");
    }

    private long calculateScore(PopularitySnapshot snapshot) {
        return snapshot.likeCount() * properties.scoreLikeWeight()
                + snapshot.commentCount() * properties.scoreCommentWeight()
                + snapshot.viewCount() * properties.scoreViewWeight();
    }
}
