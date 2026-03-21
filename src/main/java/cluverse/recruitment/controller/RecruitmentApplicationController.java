package cluverse.recruitment.controller;

import cluverse.common.api.response.ApiResponse;
import cluverse.common.auth.Login;
import cluverse.common.auth.LoginMember;
import cluverse.recruitment.service.RecruitmentApplicationQueryService;
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
@RequestMapping("/api/v1/recruitment-applications")
@RequiredArgsConstructor
public class RecruitmentApplicationController {

    private final RecruitmentApplicationQueryService recruitmentApplicationQueryService;
    private final RecruitmentApplicationService recruitmentApplicationService;

    @GetMapping("/me")
    public ApiResponse<RecruitmentApplicationPageResponse> getMyApplications(
            @Login LoginMember loginMember,
            @Valid @ModelAttribute RecruitmentApplicationSearchRequest request
    ) {
        return ApiResponse.ok(recruitmentApplicationQueryService.getMyApplications(loginMember.memberId(), request));
    }

    @GetMapping
    public ApiResponse<RecruitmentApplicationPageResponse> getApplications(
            @Login LoginMember loginMember,
            @RequestParam Long recruitmentId,
            @Valid @ModelAttribute RecruitmentApplicationSearchRequest request
    ) {
        return ApiResponse.ok(
                recruitmentApplicationQueryService.getApplications(loginMember.memberId(), recruitmentId, request)
        );
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RecruitmentApplicationDetailResponse> createApplication(
            @Login LoginMember loginMember,
            @RequestParam Long recruitmentId,
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

    @GetMapping("/{applicationId}")
    public ApiResponse<RecruitmentApplicationDetailResponse> getApplication(
            @Login LoginMember loginMember,
            @PathVariable Long applicationId
    ) {
        return ApiResponse.ok(recruitmentApplicationQueryService.getApplication(loginMember.memberId(), applicationId));
    }

    @PatchMapping("/{applicationId}/status")
    public ApiResponse<RecruitmentApplicationDetailResponse> updateApplicationStatus(
            @Login LoginMember loginMember,
            @PathVariable Long applicationId,
            @RequestBody @Valid RecruitmentApplicationStatusUpdateRequest request,
            HttpServletRequest httpRequest
    ) {
        return ApiResponse.ok(
                recruitmentApplicationService.updateApplicationStatus(
                        loginMember.memberId(),
                        applicationId,
                        request,
                        httpRequest.getRemoteAddr()
                )
        );
    }

    @DeleteMapping("/{applicationId}")
    public ApiResponse<Void> cancelApplication(@Login LoginMember loginMember,
                                               @PathVariable Long applicationId,
                                               HttpServletRequest httpRequest) {
        recruitmentApplicationService.cancelApplication(
                loginMember.memberId(),
                applicationId,
                httpRequest.getRemoteAddr()
        );
        return ApiResponse.ok();
    }

    @GetMapping("/{applicationId}/messages")
    public ApiResponse<ApplicationChatMessagePageResponse> getMessages(@Login LoginMember loginMember,
                                                                       @PathVariable Long applicationId,
                                                                       @Valid @ModelAttribute ApplicationChatMessageSearchRequest request) {
        return ApiResponse.ok(recruitmentApplicationQueryService.getMessages(loginMember.memberId(), applicationId, request));
    }

    @PostMapping("/{applicationId}/messages")
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
