package cluverse.recruitment.controller;

import cluverse.common.api.response.ApiResponse;
import cluverse.common.auth.Login;
import cluverse.common.auth.LoginMember;
import cluverse.recruitment.service.RecruitmentApplicationService;
import cluverse.recruitment.service.request.ApplicationChatMessageCreateRequest;
import cluverse.recruitment.service.request.ApplicationChatMessageSearchRequest;
import cluverse.recruitment.service.request.RecruitmentApplicationCreateRequest;
import cluverse.recruitment.service.request.RecruitmentApplicationSearchRequest;
import cluverse.recruitment.service.request.RecruitmentApplicationStatusUpdateRequest;
import cluverse.recruitment.service.response.ApplicationChatMessagePageResponse;
import cluverse.recruitment.service.response.ApplicationChatMessageResponse;
import cluverse.recruitment.service.response.RecruitmentApplicationDetailResponse;
import cluverse.recruitment.service.response.RecruitmentApplicationPageResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class RecruitmentApplicationController {

    private final RecruitmentApplicationService recruitmentApplicationService;

    @GetMapping("/api/v1/recruitment-applications/me")
    public ApiResponse<RecruitmentApplicationPageResponse> getMyApplications(
            @Login LoginMember loginMember,
            @Valid @ModelAttribute RecruitmentApplicationSearchRequest request
    ) {
        return ApiResponse.ok(recruitmentApplicationService.getMyApplications(loginMember.memberId(), request));
    }

    @GetMapping("/api/v1/recruitments/{recruitmentId}/applications")
    public ApiResponse<RecruitmentApplicationPageResponse> getApplications(
            @Login LoginMember loginMember,
            @PathVariable Long recruitmentId,
            @Valid @ModelAttribute RecruitmentApplicationSearchRequest request
    ) {
        return ApiResponse.ok(
                recruitmentApplicationService.getApplications(loginMember.memberId(), recruitmentId, request)
        );
    }

    @PostMapping("/api/v1/recruitments/{recruitmentId}/applications")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RecruitmentApplicationDetailResponse> createApplication(
            @Login LoginMember loginMember,
            @PathVariable Long recruitmentId,
            @RequestBody @Valid RecruitmentApplicationCreateRequest request,
            HttpServletRequest httpRequest
    ) {
        return ApiResponse.created(
                recruitmentApplicationService.createApplication(
                        loginMember.memberId(),
                        recruitmentId,
                        request,
                        httpRequest.getRemoteAddr()
                )
        );
    }

    @GetMapping("/api/v1/recruitments/{recruitmentId}/applications/{applicationId}")
    public ApiResponse<RecruitmentApplicationDetailResponse> getApplication(
            @Login LoginMember loginMember,
            @PathVariable Long recruitmentId,
            @PathVariable Long applicationId
    ) {
        return ApiResponse.ok(
                recruitmentApplicationService.getApplication(loginMember.memberId(), recruitmentId, applicationId)
        );
    }

    @PatchMapping("/api/v1/recruitments/{recruitmentId}/applications/{applicationId}/status")
    public ApiResponse<RecruitmentApplicationDetailResponse> updateApplicationStatus(
            @Login LoginMember loginMember,
            @PathVariable Long recruitmentId,
            @PathVariable Long applicationId,
            @RequestBody @Valid RecruitmentApplicationStatusUpdateRequest request,
            HttpServletRequest httpRequest
    ) {
        return ApiResponse.ok(
                recruitmentApplicationService.updateApplicationStatus(
                        loginMember.memberId(),
                        recruitmentId,
                        applicationId,
                        request,
                        httpRequest.getRemoteAddr()
                )
        );
    }

    @DeleteMapping("/api/v1/recruitments/{recruitmentId}/applications/{applicationId}")
    public ApiResponse<Void> cancelApplication(@Login LoginMember loginMember,
                                               @PathVariable Long recruitmentId,
                                               @PathVariable Long applicationId,
                                               HttpServletRequest httpRequest) {
        recruitmentApplicationService.cancelApplication(
                loginMember.memberId(),
                recruitmentId,
                applicationId,
                httpRequest.getRemoteAddr()
        );
        return ApiResponse.ok();
    }

    @GetMapping("/api/v1/recruitment-applications/{applicationId}/messages")
    public ApiResponse<ApplicationChatMessagePageResponse> getMessages(@Login LoginMember loginMember,
                                                                       @PathVariable Long applicationId,
                                                                       @Valid @ModelAttribute ApplicationChatMessageSearchRequest request) {
        return ApiResponse.ok(recruitmentApplicationService.getMessages(loginMember.memberId(), applicationId, request));
    }

    @PostMapping("/api/v1/recruitment-applications/{applicationId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ApplicationChatMessageResponse> createMessage(@Login LoginMember loginMember,
                                                                     @PathVariable Long applicationId,
                                                                     @RequestBody @Valid ApplicationChatMessageCreateRequest request,
                                                                     HttpServletRequest httpRequest) {
        return ApiResponse.created(
                recruitmentApplicationService.createMessage(
                        loginMember.memberId(),
                        applicationId,
                        request,
                        httpRequest.getRemoteAddr()
                )
        );
    }
}
