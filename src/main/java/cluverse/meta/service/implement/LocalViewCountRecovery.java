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
            // 복구 중 새 조회는 새 delta에 쌓이게 하고, 가져온 값만 Redis에 반영한다.
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
                // Redis 반영 여부가 확정되지 않은 값은 다음 복구에서 다시 시도할 수 있게 되돌린다.
                entry.getValue().addAndGet(delta);
                throw exception;
            }
        }
        return recovered;
    }
}
