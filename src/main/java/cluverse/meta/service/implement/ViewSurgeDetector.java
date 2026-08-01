package cluverse.meta.service.implement;

import cluverse.meta.properties.ViewSurgeProperties;
import cluverse.popularity.domain.PopularityTrigger;
import cluverse.popularity.service.implement.PopularityPromotionInvoker;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.concurrent.ConcurrentMap;

/**
 * 조회수 급상승 감지기 (조회수 증가 API V4).
 * 조회 요청에 업혀 실행된다 — 원자적 UPDATE가 돌려준 전역 누적값을
 * 관측 구간 시작 값과 비교해 증가 속도를 판정한다.
 */
@Component
public class ViewSurgeDetector {

    private static final int STALE_WINDOW_MULTIPLIER = 3;

    private final ViewSurgeProperties properties;
    private final ViewSurgeTrackingWriter viewSurgeTrackingWriter;
    private final PopularityPromotionInvoker popularityPromotionInvoker;
    private final Clock clock;
    private final Cache<Long, ViewSample> sampleCache;
    private final long windowMillis;
    private final long staleWindowMillis;
    private final Counter activationCounter;
    private final Counter rebaselinedCounter;

    public ViewSurgeDetector(
            ViewSurgeProperties properties,
            ViewSurgeTrackingWriter viewSurgeTrackingWriter,
            PopularityPromotionInvoker popularityPromotionInvoker,
            Clock clock,
            MeterRegistry meterRegistry
    ) {
        this.properties = properties;
        this.viewSurgeTrackingWriter = viewSurgeTrackingWriter;
        this.popularityPromotionInvoker = popularityPromotionInvoker;
        this.clock = clock;
        this.sampleCache = Caffeine.newBuilder()
                .maximumSize(properties.sampleCacheMaxSize())
                .expireAfterAccess(properties.sampleCacheTtl())
                .build();
        this.windowMillis = properties.window().toMillis();
        this.staleWindowMillis = windowMillis * STALE_WINDOW_MULTIPLIER;
        this.activationCounter = meterRegistry.counter("view_surge.activation");
        this.rebaselinedCounter = meterRegistry.counter("view_surge.sample.rebaselined");
        Gauge.builder("view_surge.sample_cache.size", sampleCache, Cache::estimatedSize)
                .register(meterRegistry);
    }

    public void observe(Long postId, long newCount) {
        long nowMillis = clock.millis();
        ConcurrentMap<Long, ViewSample> samples = sampleCache.asMap();

        ViewSample sample = samples.putIfAbsent(postId, new ViewSample(newCount, nowMillis));
        if (sample == null) {
            return;
        }

        long elapsedMillis = nowMillis - sample.startedAtMillis();
        if (elapsedMillis < windowMillis) {
            return;
        }

        // 기준점 교체(CAS)에 성공한 요청만 판정한다 — 동시 요청의 중복 판정·UPSERT 방지
        if (!samples.replace(postId, sample, new ViewSample(newCount, nowMillis))) {
            return;
        }

        // 관측 구간을 크게 초과했다면 판정하지 않는다 — 재려는 것은 증가 "량"이 아니라 "속도"다
        if (elapsedMillis > staleWindowMillis) {
            rebaselinedCounter.increment();
            return;
        }

        if (newCount - sample.startCount() >= properties.threshold()) {
            viewSurgeTrackingWriter.activate(postId, clock.instant());
            popularityPromotionInvoker.tryEvaluate(postId, PopularityTrigger.SURGE_ACTIVATED);
            activationCounter.increment();
        }
    }

    private record ViewSample(long startCount, long startedAtMillis) {
    }
}
