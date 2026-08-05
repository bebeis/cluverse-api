package cluverse.popularity.scheduler;

import cluverse.popularity.properties.PopularityProperties;
import cluverse.popularity.service.implement.PopularityBatchProcessorV1;
import cluverse.popularity.service.implement.PopularityFinalizationProcessor;
import cluverse.popularity.service.implement.PopularityPolicyRefreshProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PopularityScheduler {

    private final PopularityFinalizationProcessor popularityFinalizationProcessor;
    private final PopularityPolicyRefreshProcessor popularityPolicyRefreshProcessor;
    private final PopularityBatchProcessorV1 popularityBatchProcessorV1;
    private final PopularityProperties properties;

    @Scheduled(
            fixedDelayString = "${popularity.finalization-interval:30s}",
            initialDelayString = "30s"
    )
    public void finalizePopularPosts() {
        runSafely("인기글 최종 점수 확정", popularityFinalizationProcessor::finalizeDue);
    }

    @Scheduled(
            cron = "${popularity.policy-refresh-cron:0 10 1 * * *}",
            zone = "Asia/Seoul"
    )
    public void refreshPolicies() {
        runSafely("게시판별 인기글 정책 갱신", popularityPolicyRefreshProcessor::refresh);
    }

    @Scheduled(
            cron = "${popularity.baseline-scan-cron:0 0 1 * * *}",
            zone = "Asia/Seoul"
    )
    public void runBaselineScan() {
        if (properties.baselineScanEnabled()) {
            runSafely("인기글 전체 집계 기준선", popularityBatchProcessorV1::run);
        }
    }

    private void runSafely(String taskName, ScheduledTask task) {
        try {
            task.run();
        } catch (Exception exception) {
            log.error("{} 실패", taskName, exception);
        }
    }

    @FunctionalInterface
    private interface ScheduledTask {
        int run();
    }
}
