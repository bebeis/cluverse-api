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

    private Timer timer(String version, String contentType, String outcome) {
        return Timer.builder(TRANSACTION_TIMER)
                .description("로컬맵 콘텐츠 쓰기 트랜잭션 경과 시간")
                .tag("version", version)
                .tag("content", contentType)
                .tag("outcome", outcome)
                .publishPercentileHistogram()
                .register(meterRegistry);
    }
}
