package cluverse.meta.service.implement;

import cluverse.meta.properties.ViewCountProperties;
import cluverse.meta.repository.DeltaViewCountRepository;
import cluverse.meta.repository.DeltaViewCountVersion;
import cluverse.meta.repository.dto.DeltaViewCountResult;
import cluverse.meta.repository.dto.ViewCountDelta;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DeltaViewCountCounter {

    private final DeltaViewCountRepository deltaViewCountRepository;
    private final PostMetaReader postMetaReader;
    private final PostMetaWriter postMetaWriter;
    private final ViewCountProperties properties;

    public ViewCountResult countTimeBased(Long postId, String cookieId) {
        return count(DeltaViewCountVersion.TIME_BASED, postId, cookieId, false);
    }

    public ViewCountResult countThreshold(Long postId, String cookieId) {
        return count(DeltaViewCountVersion.THRESHOLD, postId, cookieId, true);
    }

    public int flushTimeBased() {
        int flushed = 0;
        for (Long postId : deltaViewCountRepository.findPostIds(DeltaViewCountVersion.TIME_BASED)) {
            if (flush(DeltaViewCountVersion.TIME_BASED, postId)) {
                flushed++;
            }
        }
        return flushed;
    }

    private ViewCountResult count(
            DeltaViewCountVersion version,
            Long postId,
            String cookieId,
            boolean flushAtThreshold
    ) {
        DeltaViewCountResult result = deltaViewCountRepository.count(version, postId, cookieId);
        if (flushAtThreshold && result.delta() >= properties.threshold()) {
            flush(version, postId);
        }
        long viewCount = postMetaReader.readViewCount(postId) + currentDelta(version, postId, result.delta());
        return new ViewCountResult(viewCount, result.counted(), ViewCountSource.REDIS_DELTA);
    }

    private long currentDelta(DeltaViewCountVersion version, Long postId, long observedDelta) {
        if (version == DeltaViewCountVersion.TIME_BASED) {
            return observedDelta;
        }
        return observedDelta >= properties.threshold() ? 0L : observedDelta;
    }

    private boolean flush(DeltaViewCountVersion version, Long postId) {
        long delta = deltaViewCountRepository.take(version, postId);
        if (delta == 0) {
            return false;
        }
        try {
            postMetaWriter.applyViewCountDeltas(List.of(new ViewCountDelta(postId, delta)));
            return true;
        } catch (RuntimeException exception) {
            deltaViewCountRepository.restore(version, postId, delta);
            throw exception;
        }
    }
}
