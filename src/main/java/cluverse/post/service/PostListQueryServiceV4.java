package cluverse.post.service;

import cluverse.board.service.implement.BoardReader;
import cluverse.post.repository.dto.PostPageQueryResult;
import cluverse.post.repository.dto.PostSummaryQueryDto;
import cluverse.post.service.implement.PostReader;
import cluverse.post.service.request.PostCursorDirection;
import cluverse.post.service.request.PostCursorSearchRequest;
import cluverse.post.service.response.PostCursorPageResponse;
import cluverse.post.service.response.PostCursorResponse;
import cluverse.post.service.response.PostSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PostListQueryServiceV4 {

    private final PostReader postReader;
    private final BoardReader boardReader;

    public PostCursorPageResponse getPosts(Long memberId, PostCursorSearchRequest request) {
        boardReader.validateReadable(memberId, request.boardId());

        PostPageQueryResult queryResult = postReader.readPostPageByCursor(memberId, request);
        List<PostSummaryResponse> responses = queryResult.posts().stream()
                .map(PostSummaryResponse::from)
                .toList();

        return new PostCursorPageResponse(
                responses,
                request.sizeOrDefault(),
                resolveHasNext(request, queryResult),
                resolveHasPrev(request, queryResult),
                toCursor(firstOf(queryResult.posts())),
                toCursor(lastOf(queryResult.posts()))
        );
    }

    private boolean resolveHasNext(PostCursorSearchRequest request, PostPageQueryResult queryResult) {
        if (isPrevMove(request)) {
            return true;
        }
        return queryResult.hasNext();
    }

    private boolean resolveHasPrev(PostCursorSearchRequest request, PostPageQueryResult queryResult) {
        if (request.hasCursor()) {
            return isPrevMove(request) ? queryResult.hasNext() : true;
        }
        if (request.isDateAnchored()) {
            return postReader.existsPostsNewerThan(request);
        }
        return false;
    }

    private boolean isPrevMove(PostCursorSearchRequest request) {
        return request.hasCursor() && request.directionOrDefault() == PostCursorDirection.PREV;
    }

    private PostCursorResponse toCursor(PostSummaryQueryDto post) {
        return post == null ? null : new PostCursorResponse(post.createdAt(), post.postId());
    }

    private PostSummaryQueryDto firstOf(List<PostSummaryQueryDto> posts) {
        return posts.isEmpty() ? null : posts.getFirst();
    }

    private PostSummaryQueryDto lastOf(List<PostSummaryQueryDto> posts) {
        return posts.isEmpty() ? null : posts.getLast();
    }
}
