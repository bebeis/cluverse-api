package cluverse.notification.service.response;

import cluverse.notification.domain.NotificationPreference;

public record NotificationPreferenceResponse(
        boolean comments,
        boolean groups,
        boolean announcements,
        boolean follows,
        boolean marketing
) {
    public static NotificationPreferenceResponse from(NotificationPreference preference) {
        return new NotificationPreferenceResponse(
                preference.isComments(),
                preference.isGroups(),
                preference.isAnnouncements(),
                preference.isFollows(),
                preference.isMarketing()
        );
    }
}
