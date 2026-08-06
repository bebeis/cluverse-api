package cluverse.place.service.implement;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class LocalMapMetricsRecorder {

    private static final String TRANSACTION_TIMER = "local.map.write.transaction.duration";
    private static final String ASYNC_TIMER = "local.map.place.async.duration";

    private final MeterRegistry meterRegistry;

    public <T> T recordTransaction(String version, String contentType, Supplier<T> operation) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            T result = operation.get();
            sample.stop(timer(version, contentType, "success"));
            return result;
        } catch (RuntimeException e) {
            sample.stop(timer(version, contentType, "failure"));
            throw e;
        }
    }

    public <T> T recordAsync(String stage, Supplier<T> operation) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            T result = operation.get();
            sample.stop(asyncTimer(stage, "success"));
            return result;
        } catch (RuntimeException e) {
            sample.stop(asyncTimer(stage, "failure"));
            throw e;
        }
    }

    public void recordAsync(String stage, Runnable operation) {
        recordAsync(stage, () -> {
            operation.run();
            return null;
        });
    }

    private Timer timer(String version, String contentType, String outcome) {
        return Timer.builder(TRANSACTION_TIMER)
                .description("로컬맵 콘텐츠 쓰기 트랜잭션 경과 시간")
                .tag("version", version)
                .tag("content", contentType)
                .tag("outcome", outcome)
                .publishPercentileHistogram()
                .register(meterRegistry);
    }

    private Timer asyncTimer(String stage, String outcome) {
        return Timer.builder(ASYNC_TIMER)
                .description("커밋 이후 비동기 장소 검증과 완료 저장 경과 시간")
                .tag("stage", stage)
                .tag("outcome", outcome)
                .publishPercentileHistogram()
                .register(meterRegistry);
    }
}
