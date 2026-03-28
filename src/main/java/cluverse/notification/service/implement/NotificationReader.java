package cluverse.notification.service.implement;

import cluverse.common.exception.ForbiddenException;
import cluverse.common.exception.NotFoundException;
import cluverse.notification.domain.Notification;
import cluverse.notification.exception.NotificationExceptionMessage;
import cluverse.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationReader {

    private final NotificationRepository notificationRepository;

    public List<Notification> readNotifications(Long memberId) {
        return notificationRepository.findAllByMemberIdOrderByCreatedAtDescIdDesc(memberId);
    }

    public Notification readOwnedNotification(Long memberId, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotFoundException(NotificationExceptionMessage.NOTIFICATION_NOT_FOUND.getMessage()));
        if (!notification.getMemberId().equals(memberId)) {
            throw new ForbiddenException(NotificationExceptionMessage.NOTIFICATION_ACCESS_DENIED.getMessage());
        }
        return notification;
    }
}
