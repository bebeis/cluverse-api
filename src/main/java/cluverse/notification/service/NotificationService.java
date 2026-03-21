package cluverse.notification.service;

import cluverse.auth.exception.AuthExceptionMessage;
import cluverse.common.exception.UnauthorizedException;
import cluverse.notification.domain.Notification;
import cluverse.notification.domain.NotificationPreference;
import cluverse.notification.service.implement.NotificationPreferenceManager;
import cluverse.notification.service.implement.NotificationWriter;
import cluverse.notification.service.request.NotificationPreferenceUpdateRequest;
import cluverse.notification.service.response.NotificationPreferenceResponse;
import cluverse.notification.service.response.NotificationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {

    private final NotificationWriter notificationWriter;
    private final NotificationPreferenceManager notificationPreferenceManager;

    public void readAll(Long memberId) {
        validateAuthenticated(memberId);
        notificationWriter.readAll(memberId);
    }

    public NotificationResponse read(Long memberId, Long notificationId) {
        validateAuthenticated(memberId);
        Notification notification = notificationWriter.read(memberId, notificationId);
        return NotificationResponse.from(notification);
    }

    public NotificationPreferenceResponse updatePreferences(Long memberId, NotificationPreferenceUpdateRequest request) {
        validateAuthenticated(memberId);
        NotificationPreference preference = notificationPreferenceManager.update(memberId, request);
        return NotificationPreferenceResponse.from(preference);
    }

    private void validateAuthenticated(Long memberId) {
        if (memberId == null) {
            throw new UnauthorizedException(AuthExceptionMessage.UNAUTHORIZED.getMessage());
        }
    }
}
