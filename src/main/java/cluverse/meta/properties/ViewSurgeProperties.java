package cluverse.meta.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 조회수 급상승 감지·Write-back 설정 (조회수 증가 API V4).
 * threshold/sustainThreshold는 단일 레코드 부하 측정(V3 계단식)에서 역산해 갱신한다.
 */
@ConfigurationProperties(prefix = "view-surge")
public record ViewSurgeProperties(
        Duration window,
        long threshold,
        long sustainThreshold,
        Duration trackingTtl,
        Duration extension,
        Duration grace,
        int sampleCacheMaxSize,
        Duration sampleCacheTtl,
        int routingCacheMaxSize,
        int cleanupBatchSize
) {
}
