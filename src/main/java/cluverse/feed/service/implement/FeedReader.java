package cluverse.feed.service.implement;

import cluverse.common.exception.BadRequestException;
import cluverse.feed.exception.FeedExceptionMessage;
import cluverse.feed.repository.FeedQueryRepository;
import cluverse.feed.repository.dto.FeedPageQueryResult;
import cluverse.feed.service.request.FollowingFeedSearchRequest;
import cluverse.feed.service.request.HomeFeedSearchRequest;
import cluverse.feed.service.request.TrendingPostSearchRequest;
import cluverse.feed.service.request.TrendingRangeType;
import cluverse.feed.service.response.FeedPageResponse;
import cluverse.feed.service.response.FeedPostSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedReader {

    private static final String CURSOR_DELIMITER = "\\|";

    private final FeedQueryRepository feedQueryRepository;

    public FeedPageResponse readHomeFeed(Long memberId, HomeFeedSearchRequest request) {
        LatestFeedCursor cursor = parseLatestCursor(request.cursor());
        Set<Long> subscribedBoardIds = feedQueryRepository.findSubscribedBoardIds(memberId);
        Set<Long> blockedMemberIds = feedQueryRepository.findBlockedMemberIds(memberId);
        Set<Long> readableGroupBoardIds = feedQueryRepository.findMyGroupBoardIds(memberId);

        FeedPageQueryResult queryResult = feedQueryRepository.findHomeFeed(
                memberId,
                request.filterOrDefault(),
                feedQueryRepository.findUniversityId(memberId),
                subscribedBoardIds,
                blockedMemberIds,
                readableGroupBoardIds,
                cursor.createdAt(),
                cursor.postId(),
                request.limitOrDefault()
        );
        return toFeedPageResponse(queryResult, request.limitOrDefault());
    }

    public FeedPageResponse readFollowingFeed(Long memberId, FollowingFeedSearchRequest request) {
        LatestFeedCursor cursor = parseLatestCursor(request.cursor());
        Set<Long> followingMemberIds = feedQueryRepository.findFollowingMemberIds(memberId);
        Set<Long> myGroupBoardIds = feedQueryRepository.findMyGroupBoardIds(memberId);
        Set<Long> blockedMemberIds = feedQueryRepository.findBlockedMemberIds(memberId);

        FeedPageQueryResult queryResult = feedQueryRepository.findFollowingFeed(
                memberId,
                request.scopeOrDefault(),
                followingMemberIds,
                myGroupBoardIds,
                blockedMemberIds,
                cursor.createdAt(),
                cursor.postId(),
                request.limitOrDefault()
        );
        return toFeedPageResponse(queryResult, request.limitOrDefault());
    }

    public FeedPageResponse readTrendingFeed(Long memberId, TrendingPostSearchRequest request) {
        TrendingFeedCursor cursor = parseTrendingCursor(request.cursor());
        Set<Long> blockedMemberIds = feedQueryRepository.findBlockedMemberIds(memberId);
        Set<Long> readableGroupBoardIds = feedQueryRepository.findMyGroupBoardIds(memberId);

        FeedPageQueryResult queryResult = feedQueryRepository.findTrendingFeed(
                memberId,
                resolveTrendingStartDateTime(request.rangeOrDefault()),
                request.category(),
                blockedMemberIds,
                readableGroupBoardIds,
                cursor.score(),
                cursor.createdAt(),
                cursor.postId(),
                request.limitOrDefault()
        );
        return toFeedPageResponse(queryResult, request.limitOrDefault());
    }

    private FeedPageResponse toFeedPageResponse(FeedPageQueryResult queryResult, int limit) {
        return new FeedPageResponse(
                queryResult.posts().stream()
                        .map(FeedPostSummaryResponse::from)
                        .toList(),
                queryResult.nextCursor(),
                limit,
                queryResult.hasNext()
        );
    }

    private LatestFeedCursor parseLatestCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return LatestFeedCursor.empty();
        }

        String[] values = cursor.split(CURSOR_DELIMITER);
        if (values.length != 2) {
            throw new BadRequestException(FeedExceptionMessage.INVALID_CURSOR.getMessage());
        }
        try {
            return new LatestFeedCursor(
                    LocalDateTime.parse(values[0]),
                    Long.parseLong(values[1])
            );
        } catch (RuntimeException exception) {
            throw new BadRequestException(FeedExceptionMessage.INVALID_CURSOR.getMessage());
        }
    }

    private TrendingFeedCursor parseTrendingCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return TrendingFeedCursor.empty();
        }

        String[] values = cursor.split(CURSOR_DELIMITER);
        if (values.length != 3) {
            throw new BadRequestException(FeedExceptionMessage.INVALID_CURSOR.getMessage());
        }
        try {
            return new TrendingFeedCursor(
                    Long.parseLong(values[0]),
                    LocalDateTime.parse(values[1]),
                    Long.parseLong(values[2])
            );
        } catch (RuntimeException exception) {
            throw new BadRequestException(FeedExceptionMessage.INVALID_CURSOR.getMessage());
        }
    }

    private LocalDateTime resolveTrendingStartDateTime(TrendingRangeType rangeType) {
        LocalDateTime now = LocalDateTime.now();
        return switch (rangeType) {
            case DAY_1 -> now.minusDays(1);
            case DAY_7 -> now.minusDays(7);
            case DAY_30 -> now.minusDays(30);
        };
    }

    private record LatestFeedCursor(
            LocalDateTime createdAt,
            Long postId
    ) {
        private static LatestFeedCursor empty() {
            return new LatestFeedCursor(null, null);
        }
    }

    private record TrendingFeedCursor(
            Long score,
            LocalDateTime createdAt,
            Long postId
    ) {
        private static TrendingFeedCursor empty() {
            return new TrendingFeedCursor(null, null, null);
        }
    }
}
