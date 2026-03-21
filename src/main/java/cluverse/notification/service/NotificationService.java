package cluverse.notification.service;

import cluverse.auth.exception.AuthExceptionMessage;
import cluverse.common.exception.ForbiddenException;
import cluverse.common.exception.UnauthorizedException;
import cluverse.notification.domain.Notification;
import cluverse.notification.domain.NotificationPreference;
import cluverse.notification.exception.NotificationExceptionMessage;
import cluverse.notification.service.implement.NotificationPreferenceManager;
import cluverse.notification.service.implement.NotificationReader;
import cluverse.notification.service.implement.NotificationWriter;
import cluverse.notification.service.request.NotificationPreferenceUpdateRequest;
import cluverse.notification.service.response.NotificationPreferenceResponse;
import cluverse.notification.service.response.NotificationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {

    private final NotificationReader notificationReader;
    private final NotificationWriter notificationWriter;
    private final NotificationPreferenceManager notificationPreferenceManager;

    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotifications(Long memberId) {
        validateAuthenticated(memberId);
        return notificationReader.readNotifications(memberId).stream()
                .map(NotificationResponse::from)
                .toList();
    }

    public void readAll(Long memberId) {
        validateAuthenticated(memberId);
        notificationWriter.readAll(memberId);
    }

    public NotificationResponse read(Long memberId, Long notificationId) {
        validateAuthenticated(memberId);
        Notification notification = notificationWriter.read(memberId, notificationId);
        return NotificationResponse.from(notification);
    }

    @Transactional(readOnly = true)
    public NotificationPreferenceResponse getPreferences(Long memberId) {
        validateAuthenticated(memberId);
        return NotificationPreferenceResponse.from(notificationPreferenceManager.readOrCreate(memberId));
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
