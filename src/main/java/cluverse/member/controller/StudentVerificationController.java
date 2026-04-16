package cluverse.member.controller;

import cluverse.common.api.response.ApiResponse;
import cluverse.common.auth.Login;
import cluverse.common.auth.LoginMember;
import cluverse.member.service.StudentVerificationQueryService;
import cluverse.member.service.StudentVerificationService;
import cluverse.member.service.request.StudentVerificationEmailChallengeCreateRequest;
import cluverse.member.service.request.StudentVerificationEmailConfirmationCreateRequest;
import cluverse.member.service.response.StudentVerificationEmailChallengeResponse;
import cluverse.member.service.response.StudentVerificationStatusResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/members/me/student-verification")
@RequiredArgsConstructor
public class StudentVerificationController {

    private final StudentVerificationQueryService studentVerificationQueryService;
    private final StudentVerificationService studentVerificationService;

    @GetMapping
    public ApiResponse<StudentVerificationStatusResponse> getMyVerificationStatus(@Login LoginMember loginMember) {
        return ApiResponse.ok(studentVerificationQueryService.getVerificationStatus(loginMember.memberId()));
    }

    @PostMapping("/email-challenges")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<StudentVerificationEmailChallengeResponse> createEmailChallenge(
            @Login LoginMember loginMember,
            @RequestBody @Valid StudentVerificationEmailChallengeCreateRequest request
    ) {
        return ApiResponse.created(studentVerificationService.createEmailChallenge(loginMember.memberId(), request));
    }

    @PostMapping("/email-challenges/{challengeId}/confirmations")
    public ApiResponse<StudentVerificationStatusResponse> createEmailConfirmation(
            @Login LoginMember loginMember,
            @PathVariable String challengeId,
            @RequestBody @Valid StudentVerificationEmailConfirmationCreateRequest request
    ) {
        return ApiResponse.ok(
                studentVerificationService.createEmailConfirmation(loginMember.memberId(), challengeId, request)
        );
    }
}
