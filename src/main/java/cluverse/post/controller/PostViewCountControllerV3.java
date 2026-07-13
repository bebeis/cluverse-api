package cluverse.post.controller;

import cluverse.common.api.response.ApiResponse;
import cluverse.post.service.PostViewCountServiceV3;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * [V3] 원자적 UPDATE 조회수 증가. 운영 방식과 동일한 구현에 위임한다.
 */
@RestController
@RequestMapping("/api/v3/posts/{postId}/view-count")
@RequiredArgsConstructor
public class PostViewCountControllerV3 {

    private final PostViewCountServiceV3 postViewCountService;

    @PostMapping
    public ApiResponse<Void> increaseViewCount(@PathVariable Long postId) {
        postViewCountService.increaseViewCount(postId);
        return ApiResponse.ok();
    }
}
