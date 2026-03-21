package cluverse.notification.service.implement;

import cluverse.notification.domain.Notification;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class NotificationWriter {

    private final NotificationReader notificationReader;

    public void readAll(Long memberId) {
        notificationReader.readNotifications(memberId)
                .forEach(Notification::markRead);
    }

    public Notification read(Long memberId, Long notificationId) {
        Notification notification = notificationReader.readOwnedNotification(memberId, notificationId);
        notification.markRead();
        return notification;
    }
}
