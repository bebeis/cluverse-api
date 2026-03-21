package cluverse.reaction.service;

import cluverse.auth.exception.AuthExceptionMessage;
import cluverse.common.exception.UnauthorizedException;
import cluverse.feed.repository.FeedQueryRepository;
import cluverse.feed.repository.dto.FeedPageQueryResult;
import cluverse.feed.service.response.FeedPostSummaryResponse;
import cluverse.meta.service.implement.PostMetaWriter;
import cluverse.post.service.implement.PostAccessReader;
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
    private final FeedQueryRepository feedQueryRepository;
    private final PostAccessReader postAccessReader;
    private final PostMetaWriter postMetaWriter;

    public PostLikeResponse likePost(Long memberId, Long postId) {
        postAccessReader.validateReadablePost(memberId, postId);
        postReactionWriter.likePost(memberId, postId);
        postMetaWriter.increaseLikeCount(postId);
        return PostLikeResponse.like(postId);
    }

    public PostBookmarkResponse bookmarkPost(Long memberId, Long postId) {
        postAccessReader.validateReadablePost(memberId, postId);
        postReactionWriter.bookmarkPost(memberId, postId);
        postMetaWriter.increaseBookmarkCount(postId);
        return PostBookmarkResponse.bookmark(postId);
    }

    public PostBookmarkResponse removeBookmark(Long memberId, Long postId) {
        postAccessReader.validateReadablePost(memberId, postId);
        postReactionWriter.removeBookmark(memberId, postId);
        postMetaWriter.decreaseBookmarkCount(postId);
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
