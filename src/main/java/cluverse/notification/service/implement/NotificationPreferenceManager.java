package cluverse.notification.service.implement;

import cluverse.notification.domain.NotificationPreference;
import cluverse.notification.repository.NotificationPreferenceRepository;
import cluverse.notification.service.request.NotificationPreferenceUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class NotificationPreferenceManager {

    private final NotificationPreferenceRepository notificationPreferenceRepository;

    @Transactional(readOnly = true)
    public NotificationPreference readOrCreate(Long memberId) {
        return notificationPreferenceRepository.findById(memberId)
                .orElseGet(() -> notificationPreferenceRepository.save(NotificationPreference.create(memberId)));
    }

    public NotificationPreference update(Long memberId, NotificationPreferenceUpdateRequest request) {
        NotificationPreference preference = readOrCreate(memberId);
        preference.update(
                request.comments(),
                request.groups(),
                request.announcements(),
                request.follows(),
                request.marketing()
        );
        return preference;
    }
}
