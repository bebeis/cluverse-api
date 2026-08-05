package cluverse.post.controller;

import cluverse.common.api.response.ApiResponse;
import cluverse.post.service.PostViewCountServiceV1;
import cluverse.post.service.response.PostViewCountResponse;
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

    @PostMapping
    public ApiResponse<PostViewCountResponse> increaseViewCount(@PathVariable Long postId) {
        return ApiResponse.ok(postViewCountService.increaseViewCount(postId));
    }
}
