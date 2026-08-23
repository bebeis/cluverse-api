package cluverse.home.controller;

import cluverse.common.api.response.ApiResponse;
import cluverse.common.auth.Login;
import cluverse.common.auth.LoginMember;
import cluverse.home.service.HomeQueryService;
import cluverse.home.service.request.FavoriteBoardSearchRequest;
import cluverse.home.service.response.CertificationDeadlineResponse;
import cluverse.home.service.response.FavoriteBoardPageResponse;
import cluverse.home.service.response.RecentCommentedPostResponse;
import cluverse.home.service.response.UsefulSiteResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/home")
@RequiredArgsConstructor
public class HomeController {

    private final HomeQueryService homeQueryService;

    @GetMapping("/favorite-boards")
    public ApiResponse<FavoriteBoardPageResponse> getFavoriteBoards(
            @Login LoginMember loginMember,
            @Valid @ModelAttribute FavoriteBoardSearchRequest request
    ) {
        return ApiResponse.ok(homeQueryService.getFavoriteBoards(loginMember.memberId(), request));
    }

    @GetMapping("/recent-commented-posts")
    public ApiResponse<List<RecentCommentedPostResponse>> getRecentCommentedPosts(
            @Login LoginMember loginMember
    ) {
        return ApiResponse.ok(homeQueryService.getRecentCommentedPosts(loginMember.memberId()));
    }

    @GetMapping("/certification-deadlines")
    public ApiResponse<List<CertificationDeadlineResponse>> getCertificationDeadlines() {
        return ApiResponse.ok(homeQueryService.getUpcomingCertificationDeadlines());
    }

    @GetMapping("/useful-sites")
    public ApiResponse<List<UsefulSiteResponse>> getUsefulSites() {
        return ApiResponse.ok(homeQueryService.getUsefulSites());
    }
}
