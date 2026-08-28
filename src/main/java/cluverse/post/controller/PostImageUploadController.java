package cluverse.post.controller;

import cluverse.common.api.response.ApiResponse;
import cluverse.common.auth.Login;
import cluverse.common.auth.LoginMember;
import cluverse.post.service.PostImageUploadService;
import cluverse.post.service.request.PostImageUploadRequest;
import cluverse.post.service.response.PostImageUploadResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/image-uploads")
@RequiredArgsConstructor
public class PostImageUploadController {

    private final PostImageUploadService postImageUploadService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PostImageUploadResponse> upload(
            @Login LoginMember loginMember,
            @Valid @ModelAttribute PostImageUploadRequest request
    ) {
        return ApiResponse.created(postImageUploadService.upload(loginMember.memberId(), request));
    }
}
