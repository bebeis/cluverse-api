package cluverse.recruitment.controller;

import cluverse.common.api.response.ApiResponse;
import cluverse.common.auth.Login;
import cluverse.common.auth.LoginMember;
import cluverse.recruitment.service.RecruitmentQueryService;
import cluverse.recruitment.service.RecruitmentService;
import cluverse.recruitment.service.request.RecruitmentCreateRequest;
import cluverse.recruitment.service.request.RecruitmentSearchRequest;
import cluverse.recruitment.service.request.RecruitmentStatusUpdateRequest;
import cluverse.recruitment.service.request.RecruitmentUpdateRequest;
import cluverse.recruitment.service.response.RecruitmentDetailResponse;
import cluverse.recruitment.service.response.RecruitmentPageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/recruitments")
@RequiredArgsConstructor
public class RecruitmentController {

    private final RecruitmentQueryService recruitmentQueryService;
    private final RecruitmentService recruitmentService;

    @GetMapping
    public ApiResponse<RecruitmentPageResponse> getRecruitments(@Login LoginMember loginMember,
                                                                @Valid @ModelAttribute RecruitmentSearchRequest request) {
        return ApiResponse.ok(recruitmentQueryService.getRecruitments(extractMemberId(loginMember), request));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RecruitmentDetailResponse> createRecruitment(@Login LoginMember loginMember,
                                                                    @RequestParam Long groupId,
                                                                    @RequestBody @Valid RecruitmentCreateRequest request) {
        return ApiResponse.created(recruitmentService.createRecruitment(loginMember.memberId(), groupId, request));
    }

    @GetMapping("/{recruitmentId}")
    public ApiResponse<RecruitmentDetailResponse> getRecruitment(@Login LoginMember loginMember,
                                                                 @PathVariable Long recruitmentId) {
        return ApiResponse.ok(recruitmentQueryService.getRecruitment(extractMemberId(loginMember), recruitmentId));
    }

    @PutMapping("/{recruitmentId}")
    public ApiResponse<RecruitmentDetailResponse> updateRecruitment(@Login LoginMember loginMember,
                                                                    @PathVariable Long recruitmentId,
                                                                    @RequestBody @Valid RecruitmentUpdateRequest request) {
        return ApiResponse.ok(recruitmentService.updateRecruitment(loginMember.memberId(), recruitmentId, request));
    }

    @PatchMapping("/{recruitmentId}/status")
    public ApiResponse<RecruitmentDetailResponse> updateRecruitmentStatus(@Login LoginMember loginMember,
                                                                          @PathVariable Long recruitmentId,
                                                                          @RequestBody @Valid RecruitmentStatusUpdateRequest request) {
        return ApiResponse.ok(
                recruitmentService.updateRecruitmentStatus(loginMember.memberId(), recruitmentId, request)
        );
    }

    @DeleteMapping("/{recruitmentId}")
    public ApiResponse<Void> deleteRecruitment(@Login LoginMember loginMember,
                                               @PathVariable Long recruitmentId) {
        recruitmentService.deleteRecruitment(loginMember.memberId(), recruitmentId);
        return ApiResponse.ok();
    }

    private Long extractMemberId(LoginMember loginMember) {
        return loginMember == null ? null : loginMember.memberId();
    }
}
