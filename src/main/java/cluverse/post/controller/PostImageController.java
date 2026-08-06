package cluverse.post.controller;

import cluverse.common.api.response.ApiResponse;
import cluverse.common.auth.Login;
import cluverse.common.auth.LoginMember;
import cluverse.post.service.PostImageService;
import cluverse.post.service.request.PostImageMultipartUploadRequest;
import cluverse.post.service.request.PostImagePresignedUrlRequest;
import cluverse.post.service.response.PostImageMultipartUploadResponse;
import cluverse.post.service.response.PostImagePresignedUrlResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/post-images")
@RequiredArgsConstructor
public class PostImageController {

    private final PostImageService postImageService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PostImageMultipartUploadResponse> upload(
            @Login LoginMember loginMember,
            @Valid @ModelAttribute PostImageMultipartUploadRequest request
    ) {
        return ApiResponse.created(postImageService.upload(loginMember.memberId(), request));
    }

    @PostMapping("/presigned-urls")
    public ApiResponse<PostImagePresignedUrlResponse> createPresignedUrl(
            @Login LoginMember loginMember,
            @RequestBody @Valid PostImagePresignedUrlRequest request
    ) {
        return ApiResponse.ok(postImageService.createPresignedUrl(loginMember.memberId(), request));
    }
}
