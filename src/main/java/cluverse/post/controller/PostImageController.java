package cluverse.post.controller;

import cluverse.common.api.response.ApiResponse;
import cluverse.common.auth.Login;
import cluverse.common.auth.LoginMember;
import cluverse.post.service.PostImageService;
import cluverse.post.service.request.PostImagePresignedUrlRequest;
import cluverse.post.service.response.PostImagePresignedUrlResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/post-images")
@RequiredArgsConstructor
public class PostImageController {

    private final PostImageService postImageService;

    @PostMapping("/presigned-urls")
    public ApiResponse<PostImagePresignedUrlResponse> createPresignedUrl(
            @Login LoginMember loginMember,
            @RequestBody @Valid PostImagePresignedUrlRequest request
    ) {
        return ApiResponse.ok(postImageService.createPresignedUrl(loginMember.memberId(), request));
    }
}
