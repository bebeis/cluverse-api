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

/**
 * [V4] 날짜 앵커 + (created_at, post_id) 커서 기반 목록 조회.
 * 겉으로는 페이지 이동처럼 보이지만 offset 없이 인덱스 시작 지점으로 바로 내려간다.
 * 카운트 쿼리 자체가 없다.
 */
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

    /**
     * hasNext = 더 과거 글 존재. PREV 이동은 커서보다 과거에서 온 것이므로 항상 true,
     * 그 외(진입/NEXT)는 최신순 조회의 슬라이스 초과분으로 판단한다.
     */
    private boolean resolveHasNext(PostCursorSearchRequest request, PostPageQueryResult queryResult) {
        if (isPrevMove(request)) {
            return true;
        }
        return queryResult.hasNext();
    }

    /**
     * hasPrev = 더 최신 글 존재. NEXT 이동은 커서 위쪽 페이지에서 온 것이므로 항상 true,
     * PREV 이동은 슬라이스 초과분, date 진입은 앵커보다 최신 글 존재 여부,
     * 무앵커 진입(최신 페이지)은 false.
     */
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
