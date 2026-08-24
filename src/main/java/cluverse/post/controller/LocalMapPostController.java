package cluverse.post.controller;

import cluverse.common.api.response.ApiResponse;
import cluverse.common.auth.Login;
import cluverse.common.auth.LoginMember;
import cluverse.post.service.LocalMapPostWriteService;
import cluverse.post.service.request.PostWithPlacesCreateRequest;
import cluverse.post.service.response.PostWithPlacesCreateResponse;
import cluverse.post.service.response.PostPlaceVerificationResponse;
import cluverse.post.service.implement.PostPlaceVerificationWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class LocalMapPostController {

    private final LocalMapPostWriteService service;
    private final PostPlaceVerificationWriter verificationWriter;

    @PostMapping("/with-places")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PostWithPlacesCreateResponse> create(
            @Login LoginMember loginMember,
            @RequestBody @Valid PostWithPlacesCreateRequest request,
            HttpServletRequest httpRequest
    ) {
        Long postId = service.create(loginMember.memberId(), request, httpRequest.getRemoteAddr());
        return ApiResponse.created(new PostWithPlacesCreateResponse(postId));
    }

    @GetMapping("/{postId}/place-verification")
    public ApiResponse<PostPlaceVerificationResponse> readVerificationStatus(
            @PathVariable Long postId
    ) {
        return ApiResponse.ok(PostPlaceVerificationResponse.from(
                verificationWriter.readOrNull(postId)));
    }
}
