package cluverse.reaction.service;

import cluverse.auth.exception.AuthExceptionMessage;
import cluverse.common.exception.UnauthorizedException;
import cluverse.feed.repository.FeedQueryRepository;
import cluverse.feed.repository.dto.FeedPageQueryResult;
import cluverse.feed.service.response.FeedPostSummaryResponse;
import cluverse.meta.service.PostMetaService;
import cluverse.post.service.PostAccessService;
import cluverse.reaction.service.implement.PostReactionWriter;
import cluverse.reaction.service.request.BookmarkedPostSearchRequest;
import cluverse.reaction.service.response.BookmarkedPostPageResponse;
import cluverse.reaction.service.response.PostBookmarkResponse;
import cluverse.reaction.service.response.PostLikeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class PostReactionService {

    private final PostReactionWriter postReactionWriter;
    private final PostMetaService postMetaService;
    private final PostAccessService postAccessService;
    private final FeedQueryRepository feedQueryRepository;

    public PostLikeResponse likePost(Long memberId, Long postId) {
        postAccessService.validateReadablePost(memberId, postId);
        postReactionWriter.likePost(memberId, postId);
        postMetaService.increaseLikeCount(postId);
        return PostLikeResponse.like(postId);
    }

    public PostBookmarkResponse bookmarkPost(Long memberId, Long postId) {
        postAccessService.validateReadablePost(memberId, postId);
        postReactionWriter.bookmarkPost(memberId, postId);
        postMetaService.increaseBookmarkCount(postId);
        return PostBookmarkResponse.bookmark(postId);
    }

    public PostBookmarkResponse removeBookmark(Long memberId, Long postId) {
        postAccessService.validateReadablePost(memberId, postId);
        postReactionWriter.removeBookmark(memberId, postId);
        postMetaService.decreaseBookmarkCount(postId);
        return PostBookmarkResponse.remove(postId);
    }

    @Transactional(readOnly = true)
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
