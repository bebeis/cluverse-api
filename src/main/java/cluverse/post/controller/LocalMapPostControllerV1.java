package cluverse.post.controller;

import cluverse.common.api.response.ApiResponse;
import cluverse.common.auth.Login;
import cluverse.common.auth.LoginMember;
import cluverse.place.service.implement.LocalMapExperimentAuthorizer;
import cluverse.post.service.LocalMapPostWriteServiceV1;
import cluverse.post.service.request.PostWithPlacesCreateRequestV1;
import cluverse.post.service.response.PostWithPlacesCreateResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class LocalMapPostControllerV1 {

    private final LocalMapPostWriteServiceV1 service;
    private final LocalMapExperimentAuthorizer experimentAuthorizer;

    @PostMapping("/with-places")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PostWithPlacesCreateResponse> create(
            @Login LoginMember loginMember,
            @RequestHeader(value = "X-Benchmark-Token", required = false) String benchmarkToken,
            @RequestBody @Valid PostWithPlacesCreateRequestV1 request,
            HttpServletRequest httpRequest
    ) {
        experimentAuthorizer.authorize(benchmarkToken);
        Long postId = service.create(loginMember.memberId(), request, httpRequest.getRemoteAddr());
        return ApiResponse.created(new PostWithPlacesCreateResponse(postId));
    }
}
