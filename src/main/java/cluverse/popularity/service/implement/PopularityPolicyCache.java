package cluverse.popularity.service.implement;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class PopularityPolicyCache {

    private static final int MAXIMUM_SIZE = 100_000;

    private final Cache<Long, PopularityPolicy> cache = Caffeine.newBuilder()
            .maximumSize(MAXIMUM_SIZE)
            .build();
    private final MeterRegistry meterRegistry;

    public PopularityPolicyCache(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public Optional<PopularityPolicy> get(Long boardId) {
        PopularityPolicy policy = cache.getIfPresent(boardId);
        meterRegistry.counter(policy == null
                ? "popularity.policy.cache.miss"
                : "popularity.policy.cache.hit").increment();
        return Optional.ofNullable(policy);
    }

    public void put(Long boardId, PopularityPolicy policy) {
        cache.put(boardId, policy);
    }

    public void invalidateAll() {
        cache.invalidateAll();
    }
}
