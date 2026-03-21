package cluverse.post.controller;

import cluverse.common.api.response.ApiResponse;
import cluverse.common.auth.Login;
import cluverse.common.auth.LoginMember;
import cluverse.feed.service.FeedQueryService;
import cluverse.feed.service.request.TrendingPostSearchRequest;
import cluverse.feed.service.response.FeedPageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class TrendingPostController {

    private final FeedQueryService feedQueryService;

    @GetMapping("/trending")
    public ApiResponse<FeedPageResponse> getTrendingPosts(
            @Login LoginMember loginMember,
            @Valid @ModelAttribute TrendingPostSearchRequest request
    ) {
        return ApiResponse.ok(feedQueryService.getTrendingPosts(extractMemberId(loginMember), request));
    }

    private Long extractMemberId(LoginMember loginMember) {
        return loginMember == null ? null : loginMember.memberId();
    }
}
