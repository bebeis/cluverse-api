package cluverse.popularity.service.implement;

import cluverse.popularity.domain.PopularityTrigger;
import cluverse.popularity.repository.PopularityCandidateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PopularityCandidateProcessor {

    private final PopularityCandidateRepository popularityCandidateRepository;
    private final PopularityCandidateClaimer popularityCandidateClaimer;
    private final PopularityPromotionProcessorV2 popularityPromotionProcessorV2;
    private final Clock clock;
    private final PopularityMetricsRecorder popularityMetricsRecorder;

    public int processDue() {
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), clock.getZone());
        List<PopularityCandidateClaim> claims = popularityCandidateClaimer.claimDue(now);
        int processed = 0;
        for (PopularityCandidateClaim claim : claims) {
            popularityMetricsRecorder.candidateLag(claim.dueAt(), now);
            try {
                popularityPromotionProcessorV2.evaluate(
                        claim.postId(),
                        PopularityTrigger.CANDIDATE_RECHECK
                );
                processed++;
            } catch (RuntimeException exception) {
                popularityMetricsRecorder.candidateEvaluationFailed();
                log.warn("인기글 후보 재검사 실패: postId={}", claim.postId(), exception);
            }
        }
        popularityMetricsRecorder.candidateQueueSize(popularityCandidateRepository.count());
        return processed;
    }
}
