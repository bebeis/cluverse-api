package cluverse.notification.service;

import cluverse.auth.exception.AuthExceptionMessage;
import cluverse.common.exception.ForbiddenException;
import cluverse.common.exception.UnauthorizedException;
import cluverse.notification.domain.Notification;
import cluverse.notification.domain.NotificationPreference;
import cluverse.notification.exception.NotificationExceptionMessage;
import cluverse.notification.repository.NotificationPreferenceRepository;
import cluverse.notification.repository.NotificationRepository;
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

    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceRepository notificationPreferenceRepository;

    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotifications(Long memberId) {
        validateAuthenticated(memberId);
        return notificationRepository.findAllByMemberIdOrderByCreatedAtDescIdDesc(memberId).stream()
                .map(NotificationResponse::from)
                .toList();
    }

    public void readAll(Long memberId) {
        validateAuthenticated(memberId);
        notificationRepository.findAllByMemberIdOrderByCreatedAtDescIdDesc(memberId)
                .forEach(Notification::markRead);
    }

    public NotificationResponse read(Long memberId, Long notificationId) {
        validateAuthenticated(memberId);
        Notification notification = readOwnedNotification(memberId, notificationId);
        notification.markRead();
        return NotificationResponse.from(notification);
    }

    @Transactional(readOnly = true)
    public NotificationPreferenceResponse getPreferences(Long memberId) {
        validateAuthenticated(memberId);
        return NotificationPreferenceResponse.from(readOrCreatePreference(memberId));
    }

    public NotificationPreferenceResponse updatePreferences(Long memberId, NotificationPreferenceUpdateRequest request) {
        validateAuthenticated(memberId);
        NotificationPreference preference = readOrCreatePreference(memberId);
        preference.update(
                request.comments(),
                request.groups(),
                request.announcements(),
                request.follows(),
                request.marketing()
        );
        return NotificationPreferenceResponse.from(preference);
    }

    private Notification readOwnedNotification(Long memberId, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new cluverse.common.exception.NotFoundException(NotificationExceptionMessage.NOTIFICATION_NOT_FOUND.getMessage()));
        if (!notification.getMemberId().equals(memberId)) {
            throw new ForbiddenException(NotificationExceptionMessage.NOTIFICATION_ACCESS_DENIED.getMessage());
        }
        return notification;
    }

    private NotificationPreference readOrCreatePreference(Long memberId) {
        return notificationPreferenceRepository.findById(memberId)
                .orElseGet(() -> notificationPreferenceRepository.save(NotificationPreference.create(memberId)));
    }

    private void validateAuthenticated(Long memberId) {
        if (memberId == null) {
            throw new UnauthorizedException(AuthExceptionMessage.UNAUTHORIZED.getMessage());
        }
    }
}
