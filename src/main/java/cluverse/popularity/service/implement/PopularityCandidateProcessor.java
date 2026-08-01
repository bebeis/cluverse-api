package cluverse.popularity.service.implement;

import cluverse.popularity.domain.PopularityCandidate;
import cluverse.popularity.domain.PopularityTrigger;
import cluverse.popularity.properties.PopularityProperties;
import cluverse.popularity.repository.PopularityCandidateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PopularityCandidateProcessor {

    private final PopularityCandidateRepository popularityCandidateRepository;
    private final PopularityPromotionProcessorV2 popularityPromotionProcessorV2;
    private final PopularityProperties properties;
    private final Clock clock;
    private final PopularityMetricsRecorder popularityMetricsRecorder;

    @Transactional
    public int processDue() {
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), clock.getZone());
        List<PopularityCandidate> candidates = popularityCandidateRepository.findDueForUpdate(
                now,
                properties.candidateBatchSize()
        );
        for (PopularityCandidate candidate : candidates) {
            popularityMetricsRecorder.candidateLag(candidate.getNextCheckAt(), now);
            popularityPromotionProcessorV2.evaluate(candidate.getPostId(), PopularityTrigger.CANDIDATE_RECHECK);
        }
        popularityMetricsRecorder.candidateQueueSize(popularityCandidateRepository.count());
        return candidates.size();
    }
}
