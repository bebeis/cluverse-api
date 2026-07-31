package cluverse.meta.service.implement;

import cluverse.meta.properties.ViewSurgeProperties;
import cluverse.meta.repository.ViewSurgeTrackingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional
public class ViewSurgeTrackingWriter {

    private final ViewSurgeTrackingRepository viewSurgeTrackingRepository;
    private final ViewSurgeProperties properties;
    private final Clock clock;

    public void activate(Long postId, Instant now) {
        LocalDateTime activatedAt = toLocalDateTime(now);
        viewSurgeTrackingRepository.upsertActivation(postId, activatedAt, activatedAt.plus(properties.trackingTtl()));
    }

    public int extendAll(List<Long> postIds) {
        LocalDateTime now = toLocalDateTime(clock.instant());
        return viewSurgeTrackingRepository.extendExpiryAll(postIds, now.plus(properties.extension()));
    }

    public void deactivate(Long postId) {
        viewSurgeTrackingRepository.deleteById(postId);
    }

    private LocalDateTime toLocalDateTime(Instant instant) {
        return LocalDateTime.ofInstant(instant, clock.getZone());
    }
}
