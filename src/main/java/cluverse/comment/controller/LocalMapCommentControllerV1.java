package cluverse.comment.controller;

import cluverse.comment.service.LocalMapCommentWriteServiceV1;
import cluverse.comment.service.request.CommentWithPlaceCreateRequestV1;
import cluverse.comment.service.response.CommentWithPlaceCreateResponse;
import cluverse.common.api.response.ApiResponse;
import cluverse.common.auth.Login;
import cluverse.common.auth.LoginMember;
import cluverse.place.service.implement.LocalMapExperimentAuthorizer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/comments")
@RequiredArgsConstructor
public class LocalMapCommentControllerV1 {

    private final LocalMapCommentWriteServiceV1 service;
    private final LocalMapExperimentAuthorizer experimentAuthorizer;

    @PostMapping("/with-place")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CommentWithPlaceCreateResponse> create(
            @Login LoginMember loginMember,
            @RequestHeader(value = "X-Benchmark-Token", required = false) String benchmarkToken,
            @RequestParam Long postId,
            @RequestBody @Valid CommentWithPlaceCreateRequestV1 request,
            HttpServletRequest httpRequest
    ) {
        experimentAuthorizer.authorize(benchmarkToken);
        Long commentId = service.create(loginMember.memberId(), postId, request, httpRequest.getRemoteAddr());
        return ApiResponse.created(new CommentWithPlaceCreateResponse(commentId));
    }
}
