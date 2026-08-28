package cluverse.post.service.implement;

import cluverse.post.domain.ImageUploadVersion;
import cluverse.post.exception.PostImageUploadTimeoutException;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.Semaphore;
import java.util.function.Function;
import java.util.function.Supplier;

@Component
public class PostImageUploadMetricsRecorder {

    private final MeterRegistry meterRegistry;

    public PostImageUploadMetricsRecorder(
            MeterRegistry meterRegistry,
            Semaphore postImageRemoteCallSemaphore
    ) {
        this.meterRegistry = meterRegistry;
        Gauge.builder("image.upload.remote.permits.available", postImageRemoteCallSemaphore, Semaphore::availablePermits)
                .description("외부 이미지 프로세서 호출에 남은 공통 permit 수")
                .register(meterRegistry);
    }

    public void request(ImageUploadVersion version, String outcome, long elapsedNanos) {
        timer("image.upload.request.duration", version, outcome).record(Duration.ofNanos(elapsedNanos));
    }

    public <T> T recordRequest(
            ImageUploadVersion version,
            Supplier<T> operation,
            Function<T, String> outcomeResolver
    ) {
        long startedAt = System.nanoTime();
        try {
            T result = operation.get();
            request(version, outcomeResolver.apply(result), System.nanoTime() - startedAt);
            return result;
        } catch (RuntimeException failure) {
            String outcome = failure instanceof PostImageUploadTimeoutException ? "timeout" : "failure";
            request(version, outcome, System.nanoTime() - startedAt);
            throw failure;
        }
    }

    public void remote(ImageUploadVersion version, long elapsedNanos) {
        timer("image.upload.remote.duration", version, "completed").record(Duration.ofNanos(elapsedNanos));
    }

    public void waitTime(ImageUploadVersion version, String kind, long elapsedNanos) {
        Timer.builder("image.upload.wait.duration")
                .description("외부 처리 시작 전 executor queue 또는 semaphore 대기 시간")
                .tag("version", version.value())
                .tag("kind", kind)
                .publishPercentileHistogram()
                .register(meterRegistry)
                .record(Duration.ofNanos(elapsedNanos));
    }

    public void bytes(ImageUploadVersion version, long sourceBytes, long outputBytes) {
        summary("image.upload.source.bytes", version).record(sourceBytes);
        summary("image.upload.output.bytes", version).record(outputBytes);
    }

    public void reconciled(String outcome) {
        meterRegistry.counter("image.upload.reconciliation.total", "outcome", outcome).increment();
    }

    public void temporaryFileCleanup(String outcome) {
        meterRegistry.counter("image.upload.temporary.file.cleanup.total", "outcome", outcome).increment();
    }

    private Timer timer(String name, ImageUploadVersion version, String outcome) {
        return Timer.builder(name)
                .tag("version", version.value())
                .tag("outcome", outcome)
                .publishPercentileHistogram()
                .register(meterRegistry);
    }

    private DistributionSummary summary(String name, ImageUploadVersion version) {
        return DistributionSummary.builder(name)
                .tag("version", version.value())
                .publishPercentileHistogram()
                .baseUnit("bytes")
                .register(meterRegistry);
    }
}
