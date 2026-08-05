package cluverse.post.controller;

import cluverse.common.api.response.ApiResponse;
import cluverse.post.service.PostViewCountServiceV4;
import cluverse.post.service.response.PostViewCountResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v4/posts/{postId}/view-count")
@RequiredArgsConstructor
public class PostViewCountControllerV4 {

    private final PostViewCountServiceV4 postViewCountService;
    private final ViewCountCookieResolver viewCountCookieResolver;

    @PostMapping
    public ApiResponse<PostViewCountResponse> increaseViewCount(
            @PathVariable Long postId,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String cookieId = viewCountCookieResolver.resolve(request, response);
        return ApiResponse.ok(postViewCountService.increaseViewCount(postId, cookieId));
    }
}
