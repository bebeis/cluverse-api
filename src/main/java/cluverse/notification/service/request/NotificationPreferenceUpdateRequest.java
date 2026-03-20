package cluverse.notification.service.request;

public record NotificationPreferenceUpdateRequest(
        boolean comments,
        boolean groups,
        boolean announcements,
        boolean follows,
        boolean marketing
) {
}
