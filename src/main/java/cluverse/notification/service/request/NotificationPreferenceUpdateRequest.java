package cluverse.notification.service.request;

import jakarta.validation.constraints.NotNull;

public record NotificationPreferenceUpdateRequest(
        @NotNull(message = "댓글 알림 설정을 입력해주세요.") Boolean comments,
        @NotNull(message = "그룹 알림 설정을 입력해주세요.") Boolean groups,
        @NotNull(message = "공지 알림 설정을 입력해주세요.") Boolean announcements,
        @NotNull(message = "팔로우 알림 설정을 입력해주세요.") Boolean follows,
        @NotNull(message = "마케팅 알림 설정을 입력해주세요.") Boolean marketing
) {
}
