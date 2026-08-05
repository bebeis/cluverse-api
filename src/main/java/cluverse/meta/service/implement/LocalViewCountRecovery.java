package cluverse.meta.service.implement;

import cluverse.meta.repository.TotalViewCountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Component
@RequiredArgsConstructor
public class LocalViewCountRecovery {

    private final LocalViewCountFallback localViewCountFallback;
    private final ViewCountInitializer viewCountInitializer;
    private final TotalViewCountRepository totalViewCountRepository;
    private final PostMetaReader postMetaReader;

    public long recover() {
        long recovered = 0;
        for (Map.Entry<Long, AtomicLong> entry : localViewCountFallback.deltas().entrySet()) {
            long delta = entry.getValue().getAndSet(0);
            if (delta == 0) {
                continue;
            }
            try {
                viewCountInitializer.ensureInitialized(entry.getKey());
                totalViewCountRepository.ensureAtLeast(entry.getKey(), postMetaReader.readViewCount(entry.getKey()));
                totalViewCountRepository.increaseBy(entry.getKey(), delta);
                localViewCountFallback.reflected(entry.getKey());
                recovered += delta;
            } catch (RuntimeException exception) {
                entry.getValue().addAndGet(delta);
                throw exception;
            }
        }
        return recovered;
    }
}
