package cluverse.feed.service;

import cluverse.feed.service.request.FollowingFeedSearchRequest;
import cluverse.feed.service.request.HomeFeedSearchRequest;
import cluverse.feed.service.request.TrendingPostSearchRequest;
import cluverse.feed.service.response.FeedPageResponse;

public interface FeedService {

    FeedPageResponse getHomeFeed(Long memberId, HomeFeedSearchRequest request);

    FeedPageResponse getFollowingFeed(Long memberId, FollowingFeedSearchRequest request);

    FeedPageResponse getTrendingPosts(Long memberId, TrendingPostSearchRequest request);
}
