package cluverse.post.controller;

import cluverse.common.api.response.ApiResponse;
import cluverse.common.auth.Login;
import cluverse.common.auth.LoginMember;
import cluverse.post.service.LocalMapPostWriteServiceV2;
import cluverse.post.service.request.PostWithPlacesCreateRequestV2;
import cluverse.post.service.response.PostWithPlacesCreateResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/posts")
@RequiredArgsConstructor
public class LocalMapPostControllerV2 {

    private final LocalMapPostWriteServiceV2 service;

    @PostMapping("/with-places")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PostWithPlacesCreateResponse> create(
            @Login LoginMember loginMember,
            @RequestBody @Valid PostWithPlacesCreateRequestV2 request,
            HttpServletRequest httpRequest
    ) {
        Long postId = service.create(loginMember.memberId(), request, httpRequest.getRemoteAddr());
        return ApiResponse.created(new PostWithPlacesCreateResponse(postId));
    }
}
