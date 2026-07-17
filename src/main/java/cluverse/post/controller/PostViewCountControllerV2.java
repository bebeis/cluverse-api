package cluverse.post.controller;

import cluverse.common.api.response.ApiResponse;
import cluverse.post.service.PostViewCountServiceV2;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * [V2] 비관적 락(select for update + 더티체킹) 조회수 증가.
 */
@RestController
@RequestMapping("/api/v2/posts/{postId}/view-count")
@RequiredArgsConstructor
public class PostViewCountControllerV2 {

    private final PostViewCountServiceV2 postViewCountService;

    @PostMapping
    public ApiResponse<Void> increaseViewCount(@PathVariable Long postId) {
        postViewCountService.increaseViewCount(postId);
        return ApiResponse.ok();
    }
}
