package cluverse.home.controller;

import cluverse.common.api.response.ApiResponse;
import cluverse.common.auth.Login;
import cluverse.common.auth.LoginMember;
import cluverse.home.service.HomeQueryService;
import cluverse.home.service.response.RecentCommentedPostResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v2/home")
@RequiredArgsConstructor
public class HomeControllerV2 {

    private final HomeQueryService homeQueryService;

    @GetMapping("/recent-commented-posts")
    public ApiResponse<List<RecentCommentedPostResponse>> getRecentCommentedPosts(
            @Login LoginMember loginMember
    ) {
        return ApiResponse.ok(homeQueryService.getRecentCommentedPostsV2(loginMember.memberId()));
    }
}
