package cluverse.post.controller;

import cluverse.common.api.response.ApiResponse;
import cluverse.post.domain.ImageUploadVersion;
import cluverse.post.service.PostImageUploadService;
import cluverse.post.service.request.PostImageUploadRequest;
import cluverse.post.service.response.PostImageUploadResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v3/image-uploads")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "image-upload-experiment", name = "enabled", havingValue = "true")
public class PostImageUploadControllerV3 {

    private final PostImageUploadService service;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PostImageUploadResponse> upload(
            @RequestHeader(value = "X-Benchmark-Token", required = false) String benchmarkToken,
            @Valid @ModelAttribute PostImageUploadRequest request
    ) {
        return ApiResponse.created(service.upload(ImageUploadVersion.V3, benchmarkToken, request));
    }
}
