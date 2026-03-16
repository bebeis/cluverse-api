package cluverse.feed.service;

import cluverse.feed.service.request.FollowingFeedSearchRequest;
import cluverse.feed.service.request.HomeFeedSearchRequest;
import cluverse.feed.service.request.TrendingPostSearchRequest;
import cluverse.feed.service.response.FeedPageResponse;
import org.springframework.stereotype.Service;

@Service
public class FeedDesignService implements FeedService {

    // API contract design placeholder. Query composition and ranking logic are added later.
    @Override
    public FeedPageResponse getHomeFeed(Long memberId, HomeFeedSearchRequest request) {
        return FeedPageResponse.empty(request.limitOrDefault());
    }

    @Override
    public FeedPageResponse getFollowingFeed(Long memberId, FollowingFeedSearchRequest request) {
        return FeedPageResponse.empty(request.limitOrDefault());
    }

    @Override
    public FeedPageResponse getTrendingPosts(Long memberId, TrendingPostSearchRequest request) {
        return FeedPageResponse.empty(request.limitOrDefault());
    }
}
