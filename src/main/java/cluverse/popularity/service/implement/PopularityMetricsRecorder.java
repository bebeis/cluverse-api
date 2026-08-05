package cluverse.popularity.service.implement;

import cluverse.popularity.domain.PopularityAlgorithmVersion;
import cluverse.popularity.domain.PopularityTrigger;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class PopularityMetricsRecorder {

    private final MeterRegistry meterRegistry;

    public PopularityMetricsRecorder(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordEvaluationDuration(
            PopularityAlgorithmVersion version,
            PopularityTrigger trigger,
            long elapsedNanos
    ) {
        meterRegistry.timer(
                "popularity.evaluation.duration",
                "version", version.name(),
                "trigger", trigger.name()
        ).record(Duration.ofNanos(elapsedNanos));
    }

    public void evaluated(PopularityAlgorithmVersion version, PopularityTrigger trigger, String outcome) {
        meterRegistry.counter(
                "popularity.evaluation.total",
                "version", version.name(),
                "trigger", trigger.name(),
                "outcome", outcome
        ).increment();
    }

    public void promoted(PopularityAlgorithmVersion version, PopularityTrigger trigger) {
        meterRegistry.counter(
                "popularity.promotion.total",
                "version", version.name(),
                "trigger", trigger.name()
        ).increment();
    }

    public void examined(PopularityAlgorithmVersion version, int count) {
        meterRegistry.counter("popularity.posts.examined", "version", version.name()).increment(count);
    }
}
