package cluverse.reaction.service;

import cluverse.auth.exception.AuthExceptionMessage;
import cluverse.common.exception.UnauthorizedException;
import cluverse.feed.repository.FeedQueryRepository;
import cluverse.feed.repository.dto.FeedPageQueryResult;
import cluverse.feed.service.response.FeedPostSummaryResponse;
import cluverse.reaction.service.request.BookmarkedPostSearchRequest;
import cluverse.reaction.service.response.BookmarkedPostPageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PostReactionQueryService {

    private final FeedQueryRepository feedQueryRepository;

    public BookmarkedPostPageResponse getBookmarkedPosts(Long memberId, BookmarkedPostSearchRequest request) {
        validateAuthenticated(memberId);

        FeedPageQueryResult queryResult = feedQueryRepository.findBookmarkedFeedPage(
                memberId,
                feedQueryRepository.findBlockedMemberIds(memberId),
                feedQueryRepository.findMyGroupBoardIds(memberId),
                request.sortOrDefault(),
                request.pageOrDefault(),
                request.sizeOrDefault()
        );

        List<FeedPostSummaryResponse> posts = queryResult.posts().stream()
                .map(FeedPostSummaryResponse::from)
                .toList();

        return new BookmarkedPostPageResponse(
                posts,
                request.pageOrDefault(),
                request.sizeOrDefault(),
                queryResult.hasNext(),
                request.sortOrDefault()
        );
    }

    private void validateAuthenticated(Long memberId) {
        if (memberId == null) {
            throw new UnauthorizedException(AuthExceptionMessage.UNAUTHORIZED.getMessage());
        }
    }
}
