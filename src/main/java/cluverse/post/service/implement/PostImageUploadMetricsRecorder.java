package cluverse.post.service.implement;

import cluverse.post.domain.ImageUploadVersion;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;

@Component
public class PostImageUploadMetricsRecorder {

    private final MeterRegistry meterRegistry;

    public PostImageUploadMetricsRecorder(
            MeterRegistry meterRegistry,
            @Qualifier("postImagePlatformExecutor") ThreadPoolExecutor platformExecutor,
            Semaphore postImageRemoteCallSemaphore
    ) {
        this.meterRegistry = meterRegistry;
        Gauge.builder("image.upload.platform.executor.active", platformExecutor, ThreadPoolExecutor::getActiveCount)
                .description("V2 고정 Platform executor의 실행 중 작업 수")
                .register(meterRegistry);
        Gauge.builder("image.upload.platform.executor.queue", platformExecutor, executor -> executor.getQueue().size())
                .description("V2 고정 Platform executor의 대기 작업 수")
                .register(meterRegistry);
        Gauge.builder("image.upload.remote.permits.available", postImageRemoteCallSemaphore, Semaphore::availablePermits)
                .description("외부 이미지 프로세서 호출에 남은 공통 permit 수")
                .register(meterRegistry);
    }

    public void request(ImageUploadVersion version, String outcome, long elapsedNanos) {
        timer("image.upload.request.duration", version, outcome).record(Duration.ofNanos(elapsedNanos));
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
