package cluverse.meta.service.implement;

import cluverse.meta.properties.ViewSurgeProperties;
import cluverse.meta.repository.ViewSurgeTrackingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ViewSurgeTrackingReader {

    private final ViewSurgeTrackingRepository viewSurgeTrackingRepository;
    private final ViewSurgeProperties properties;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<Long> readActivePostIds() {
        return viewSurgeTrackingRepository.findActivePostIds(LocalDateTime.now(clock), properties.routingCacheMaxSize());
    }

    @Transactional(readOnly = true)
    public List<Long> readExpiredPostIds() {
        LocalDateTime cutoff = LocalDateTime.now(clock).minus(properties.grace());
        return viewSurgeTrackingRepository.findExpiredPostIds(cutoff, properties.cleanupBatchSize());
    }
}
