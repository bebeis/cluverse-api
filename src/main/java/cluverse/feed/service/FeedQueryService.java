package cluverse.feed.service;

import cluverse.auth.exception.AuthExceptionMessage;
import cluverse.common.exception.UnauthorizedException;
import cluverse.feed.service.implement.FeedReader;
import cluverse.feed.service.request.FollowingFeedSearchRequest;
import cluverse.feed.service.request.HomeFeedSearchRequest;
import cluverse.feed.service.request.TrendingPostSearchRequest;
import cluverse.feed.service.response.FeedPageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedQueryService {
    private final FeedReader feedReader;

    public FeedPageResponse getHomeFeed(Long memberId, HomeFeedSearchRequest request) {
        return feedReader.readHomeFeed(memberId, request);
    }

    public FeedPageResponse getFollowingFeed(Long memberId, FollowingFeedSearchRequest request) {
        validateAuthenticated(memberId);
        return feedReader.readFollowingFeed(memberId, request);
    }

    public FeedPageResponse getTrendingPosts(Long memberId, TrendingPostSearchRequest request) {
        return feedReader.readTrendingFeed(memberId, request);
    }

    private void validateAuthenticated(Long memberId) {
        if (memberId == null) {
            throw new UnauthorizedException(AuthExceptionMessage.UNAUTHORIZED.getMessage());
        }
    }
}
