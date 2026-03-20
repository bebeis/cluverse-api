package cluverse.notification.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationExceptionMessage {
    NOTIFICATION_NOT_FOUND("존재하지 않는 알림입니다."),
    NOTIFICATION_ACCESS_DENIED("해당 알림에 접근할 수 없습니다.");

    private final String message;
}
