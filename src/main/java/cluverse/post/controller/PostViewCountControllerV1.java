package cluverse.post.controller;

import cluverse.common.api.response.ApiResponse;
import cluverse.post.service.PostViewCountServiceV1;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/posts/{postId}/view-count")
@RequiredArgsConstructor
public class PostViewCountControllerV1 {

    private final PostViewCountServiceV1 postViewCountService;

    @PostMapping("/v1")
    public ApiResponse<Void> increaseViewCountV1(@PathVariable Long postId) {
        postViewCountService.increaseViewCountV1(postId);
        return ApiResponse.ok();
    }

    @PostMapping("/v2")
    public ApiResponse<Void> increaseViewCountV2(@PathVariable Long postId) {
        postViewCountService.increaseViewCountV2(postId);
        return ApiResponse.ok();
    }
}
