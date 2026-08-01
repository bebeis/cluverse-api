package cluverse.comment.controller;

import cluverse.comment.service.LocalMapCommentWriteServiceV2;
import cluverse.comment.service.request.CommentWithPlaceCreateRequestV2;
import cluverse.comment.service.response.CommentWithPlaceCreateResponse;
import cluverse.common.api.response.ApiResponse;
import cluverse.common.auth.Login;
import cluverse.common.auth.LoginMember;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/comments")
@RequiredArgsConstructor
public class LocalMapCommentControllerV2 {

    private final LocalMapCommentWriteServiceV2 service;

    @PostMapping("/with-place")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CommentWithPlaceCreateResponse> create(
            @Login LoginMember loginMember,
            @RequestParam Long postId,
            @RequestBody @Valid CommentWithPlaceCreateRequestV2 request,
            HttpServletRequest httpRequest
    ) {
        Long commentId = service.create(loginMember.memberId(), postId, request, httpRequest.getRemoteAddr());
        return ApiResponse.created(new CommentWithPlaceCreateResponse(commentId));
    }
}
