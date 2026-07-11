package cluverse.notification.controller;

import cluverse.common.api.response.ApiResponse;
import cluverse.common.auth.Login;
import cluverse.common.auth.LoginMember;
import cluverse.notification.service.NotificationQueryService;
import cluverse.notification.service.NotificationService;
import cluverse.notification.service.request.NotificationPreferenceUpdateRequest;
import cluverse.notification.service.response.NotificationPreferenceResponse;
import cluverse.notification.service.response.NotificationResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationQueryService notificationQueryService;
    private final NotificationService notificationService;

    @GetMapping("/notifications")
    public ApiResponse<List<NotificationResponse>> getNotifications(@Login LoginMember loginMember) {
        return ApiResponse.ok(notificationQueryService.getNotifications(extractMemberId(loginMember)));
    }

    @PostMapping("/notifications/read-all")
    public ApiResponse<Void> readAll(@Login LoginMember loginMember) {
        notificationService.readAll(extractMemberId(loginMember));
        return ApiResponse.ok();
    }

    @PatchMapping("/notifications/{notificationId}/read")
    public ApiResponse<NotificationResponse> read(@Login LoginMember loginMember,
                                                  @PathVariable Long notificationId) {
        return ApiResponse.ok(notificationService.read(extractMemberId(loginMember), notificationId));
    }

    @GetMapping("/notification-preferences")
    public ApiResponse<NotificationPreferenceResponse> getPreferences(@Login LoginMember loginMember) {
        return ApiResponse.ok(notificationQueryService.getPreferences(extractMemberId(loginMember)));
    }

    @PutMapping("/notification-preferences")
    public ApiResponse<NotificationPreferenceResponse> updatePreferences(@Login LoginMember loginMember,
                                                                         @RequestBody @Valid NotificationPreferenceUpdateRequest request) {
        return ApiResponse.ok(notificationService.updatePreferences(extractMemberId(loginMember), request));
    }

    private Long extractMemberId(LoginMember loginMember) {
        return loginMember == null ? null : loginMember.memberId();
    }
}
