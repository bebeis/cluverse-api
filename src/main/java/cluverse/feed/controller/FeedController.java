package cluverse.feed.controller;

import cluverse.common.api.response.ApiResponse;
import cluverse.common.auth.Login;
import cluverse.common.auth.LoginMember;
import cluverse.feed.service.FeedService;
import cluverse.feed.service.request.FollowingFeedSearchRequest;
import cluverse.feed.service.request.HomeFeedSearchRequest;
import cluverse.feed.service.response.FeedPageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/feeds")
@RequiredArgsConstructor
public class FeedController {

    private final FeedService feedService;

    @GetMapping("/home")
    public ApiResponse<FeedPageResponse> getHomeFeed(
            @Login LoginMember loginMember,
            @Valid @ModelAttribute HomeFeedSearchRequest request
    ) {
        return ApiResponse.ok(feedService.getHomeFeed(extractMemberId(loginMember), request));
    }

    @GetMapping("/following")
    public ApiResponse<FeedPageResponse> getFollowingFeed(
            @Login LoginMember loginMember,
            @Valid @ModelAttribute FollowingFeedSearchRequest request
    ) {
        return ApiResponse.ok(feedService.getFollowingFeed(loginMember.memberId(), request));
    }

    private Long extractMemberId(LoginMember loginMember) {
        return loginMember == null ? null : loginMember.memberId();
    }
}
