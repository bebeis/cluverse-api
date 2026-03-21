package cluverse.notification.service;

import cluverse.auth.exception.AuthExceptionMessage;
import cluverse.common.exception.UnauthorizedException;
import cluverse.notification.service.implement.NotificationPreferenceManager;
import cluverse.notification.service.implement.NotificationReader;
import cluverse.notification.service.response.NotificationPreferenceResponse;
import cluverse.notification.service.response.NotificationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationQueryService {

    private final NotificationReader notificationReader;
    private final NotificationPreferenceManager notificationPreferenceManager;

    public List<NotificationResponse> getNotifications(Long memberId) {
        validateAuthenticated(memberId);
        return notificationReader.readNotifications(memberId).stream()
                .map(NotificationResponse::from)
                .toList();
    }

    public NotificationPreferenceResponse getPreferences(Long memberId) {
        validateAuthenticated(memberId);
        return NotificationPreferenceResponse.from(notificationPreferenceManager.readOrCreate(memberId));
    }

    private void validateAuthenticated(Long memberId) {
        if (memberId == null) {
            throw new UnauthorizedException(AuthExceptionMessage.UNAUTHORIZED.getMessage());
        }
    }
}
