package cluverse.popularity.service.implement;

import cluverse.popularity.domain.PopularityAlgorithmVersion;
import cluverse.popularity.domain.PopularityTrigger;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class PopularityMetricsRecorder {

    private final MeterRegistry meterRegistry;
    private final AtomicLong candidateQueueSize = new AtomicLong();

    public PopularityMetricsRecorder(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        Gauge.builder("popularity.candidate.queue.size", candidateQueueSize, AtomicLong::get)
                .register(meterRegistry);
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
        meterRegistry.counter("popularity.examined.candidates", "version", version.name()).increment(count);
    }

    public void candidateLag(LocalDateTime nextCheckAt, LocalDateTime checkedAt) {
        Duration lag = Duration.between(nextCheckAt, checkedAt);
        meterRegistry.timer("popularity.candidate.lag")
                .record(lag.isNegative() ? Duration.ZERO : lag);
    }

    public void candidateQueueSize(long size) {
        candidateQueueSize.set(size);
    }

    public void candidateEvaluationFailed() {
        meterRegistry.counter("popularity.candidate.evaluation.failures").increment();
    }
}
