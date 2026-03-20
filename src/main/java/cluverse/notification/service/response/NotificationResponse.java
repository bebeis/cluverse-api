package cluverse.notification.service.response;

import cluverse.notification.domain.Notification;
import cluverse.notification.domain.NotificationType;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long notificationId,
        NotificationType type,
        String title,
        String content,
        String excerpt,
        boolean isRead,
        LocalDateTime createdAt,
        String targetUrl
) {
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getContent(),
                notification.getExcerpt(),
                notification.isRead(),
                notification.getCreatedAt(),
                notification.getTargetUrl()
        );
    }
}
