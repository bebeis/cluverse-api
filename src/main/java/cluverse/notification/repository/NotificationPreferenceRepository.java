package cluverse.notification.repository;

import cluverse.notification.domain.NotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, Long> {
}
