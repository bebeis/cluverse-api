package cluverse.meta.service.implement;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * 급상승 라우팅 캐시 (조회수 증가 API V4).
 * 활성 게시글 ID 스냅샷 — 요청 경로는 락 없이 읽고, 스케줄러가 통째로 교체한다.
 */
@Component
public class ViewSurgeRoutingCache {

    private volatile Set<Long> trackedPostIds = Set.of();

    public ViewSurgeRoutingCache(MeterRegistry meterRegistry) {
        Gauge.builder("view_surge.routing_cache.size", this, cache -> cache.trackedPostIds.size())
                .register(meterRegistry);
    }

    public boolean contains(Long postId) {
        return trackedPostIds.contains(postId);
    }

    public void replace(List<Long> postIds) {
        Set<Long> current = trackedPostIds;
        if (current.size() == postIds.size() && current.containsAll(postIds)) {
            return;
        }
        trackedPostIds = Set.copyOf(postIds);
    }
}
