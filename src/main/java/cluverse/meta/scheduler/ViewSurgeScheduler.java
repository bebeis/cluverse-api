package cluverse.meta.scheduler;

import cluverse.meta.service.implement.ViewCountFlushProcessor;
import cluverse.meta.service.implement.ViewSurgeCleanupProcessor;
import cluverse.meta.service.implement.ViewSurgeRoutingCache;
import cluverse.meta.service.implement.ViewSurgeTrackingReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 급상승 추적 스케줄 진입점 (조회수 증가 API V4).
 * 한 주기의 실패가 스케줄을 죽이지 않게 각 메서드는 예외를 삼키고 로그만 남긴다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ViewSurgeScheduler {

    private final ViewSurgeTrackingReader viewSurgeTrackingReader;
    private final ViewSurgeRoutingCache viewSurgeRoutingCache;
    private final ViewCountFlushProcessor viewCountFlushProcessor;
    private final ViewSurgeCleanupProcessor viewSurgeCleanupProcessor;

    @Scheduled(fixedDelayString = "${view-surge.routing-refresh-interval:3s}", initialDelayString = "3s")
    public void refreshRoutingCache() {
        try {
            viewSurgeRoutingCache.replace(viewSurgeTrackingReader.readActivePostIds());
        } catch (Exception exception) {
            log.error("급상승 라우팅 캐시 갱신 실패", exception);
        }
    }

    @Scheduled(fixedDelayString = "${view-surge.flush-interval:3s}", initialDelayString = "5s")
    public void flushPendingViewCounts() {
        try {
            viewCountFlushProcessor.flush();
        } catch (Exception exception) {
            log.error("조회수 Write-back 플러시 실패", exception);
        }
    }

    @Scheduled(fixedDelayString = "${view-surge.cleanup-interval:10s}", initialDelayString = "10s")
    public void cleanUpExpiredTracking() {
        try {
            viewSurgeCleanupProcessor.cleanUp();
        } catch (Exception exception) {
            log.error("급상승 추적 정리 실패", exception);
        }
    }
}
