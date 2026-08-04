package cluverse.home.service;

import cluverse.certification.service.implement.CertificationScheduleReader;
import cluverse.home.service.implement.HomeReader;
import cluverse.home.service.implement.UsefulSiteReader;
import cluverse.home.service.request.FavoriteBoardSearchRequest;
import cluverse.home.service.response.CertificationDeadlineResponse;
import cluverse.home.service.response.FavoriteBoardPageResponse;
import cluverse.home.service.response.RecentCommentedPostResponse;
import cluverse.home.service.response.UsefulSiteResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HomeQueryService {

    private static final int HOME_COMPONENT_SIZE = 10;

    private final HomeReader homeReader;
    private final CertificationScheduleReader certificationScheduleReader;
    private final UsefulSiteReader usefulSiteReader;
    private final Clock clock;

    public FavoriteBoardPageResponse getFavoriteBoards(Long memberId, FavoriteBoardSearchRequest request) {
        return FavoriteBoardPageResponse.from(
                homeReader.readFavoriteBoards(memberId, request.cursor(), request.sizeOrDefault())
        );
    }

    public List<RecentCommentedPostResponse> getRecentCommentedPostsV1(Long memberId) {
        return homeReader.readRecentCommentedPostsV1(memberId, HOME_COMPONENT_SIZE).stream()
                .map(RecentCommentedPostResponse::from)
                .toList();
    }

    public List<RecentCommentedPostResponse> getRecentCommentedPostsV2(Long memberId) {
        return homeReader.readRecentCommentedPostsV2(memberId, HOME_COMPONENT_SIZE).stream()
                .map(RecentCommentedPostResponse::from)
                .toList();
    }

    public List<RecentCommentedPostResponse> getRecentCommentedPostsV3(Long memberId) {
        return homeReader.readRecentCommentedPostsV3(memberId, HOME_COMPONENT_SIZE).stream()
                .map(RecentCommentedPostResponse::from)
                .toList();
    }

    public List<CertificationDeadlineResponse> getUpcomingCertificationDeadlines() {
        return certificationScheduleReader.readUpcomingDeadlines(LocalDate.now(clock), HOME_COMPONENT_SIZE).stream()
                .map(CertificationDeadlineResponse::from)
                .toList();
    }

    public List<UsefulSiteResponse> getUsefulSites() {
        return usefulSiteReader.readAll().stream()
                .map(UsefulSiteResponse::from)
                .toList();
    }
}
