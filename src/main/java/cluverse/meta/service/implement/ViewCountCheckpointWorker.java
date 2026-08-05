package cluverse.meta.service.implement;

import cluverse.meta.repository.TotalViewCountRepository;
import cluverse.meta.repository.dto.ResidentViewCount;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class ViewCountCheckpointWorker {

    private final TotalViewCountRepository totalViewCountRepository;
    private final PostMetaWriter postMetaWriter;
    private final Clock clock;
    private final AtomicLong lastSuccessMillis = new AtomicLong();

    public ViewCountCheckpointWorker(
            TotalViewCountRepository totalViewCountRepository,
            PostMetaWriter postMetaWriter,
            Clock clock,
            MeterRegistry meterRegistry
    ) {
        this.totalViewCountRepository = totalViewCountRepository;
        this.postMetaWriter = postMetaWriter;
        this.clock = clock;
        Gauge.builder("view_count.checkpoint.lag.seconds", this, ViewCountCheckpointWorker::lagSeconds)
                .register(meterRegistry);
    }

    public int checkpoint() {
        List<ResidentViewCount> counters = totalViewCountRepository.findAll();
        postMetaWriter.checkpointViewCounts(counters.stream().map(ResidentViewCount::toSnapshot).toList());
        lastSuccessMillis.set(clock.millis());
        return counters.size();
    }

    private double lagSeconds() {
        long lastSuccess = lastSuccessMillis.get();
        return lastSuccess == 0 ? 0 : Math.max(0, clock.millis() - lastSuccess) / 1000.0;
    }
}
