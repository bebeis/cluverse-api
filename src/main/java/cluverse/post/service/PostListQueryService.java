package cluverse.post.service;

import cluverse.board.service.implement.BoardReader;
import cluverse.post.repository.dto.PostPageQueryResult;
import cluverse.post.repository.dto.PostSummaryQueryDto;
import cluverse.post.service.implement.PostPageReader;
import cluverse.post.service.implement.PostReader;
import cluverse.post.service.request.PostCursorDirection;
import cluverse.post.service.request.PostCursorSearchRequest;
import cluverse.post.service.request.PostKeywordSearchRequest;
import cluverse.post.service.request.PostPageSearchRequest;
import cluverse.post.service.request.PostSortType;
import cluverse.post.service.response.PostCursorPageResponse;
import cluverse.post.service.response.PostCursorResponse;
import cluverse.post.service.response.PostPageResponse;
import cluverse.post.service.response.PostSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PostListQueryService {

    private static final int PAGE_BLOCK_SIZE = 10;
    private static final int MAX_OFFSET_PAGE = 200;

    private final PostReader postReader;
    private final PostPageReader postPageReader;
    private final BoardReader boardReader;

    public PostPageResponse readPage(Long memberId, PostPageSearchRequest request) {
        boardReader.validateReadable(memberId, request.boardId());

        int page = request.pageOrDefault();
        int size = request.sizeOrDefault();
        long searchLimit = pageBlockSearchLimit(page, size);
        PostPageQueryResult queryResult = postPageReader.readPage(memberId, request, searchLimit);
        long cappedCount = queryResult.cappedCount()
                .orElseGet(() -> postReader.countPostsUpTo(request, searchLimit));
        PageBlock pageBlock = resolvePageBlock(page, size, cappedCount);
        boolean cursorAvailable = request.sortOrDefault() == PostSortType.LATEST;

        return new PostPageResponse(
                toResponses(queryResult.posts()),
                page,
                size,
                queryResult.hasNext(),
                pageBlock.lastPage(),
                pageBlock.hasNextBlock(),
                false,
                page > 1,
                cursorAvailable ? toCursor(firstOf(queryResult.posts())) : null,
                cursorAvailable ? toCursor(lastOf(queryResult.posts())) : null,
                cursorAvailable && page == MAX_OFFSET_PAGE && queryResult.hasNext()
        );
    }

    public PostPageResponse search(Long memberId, PostKeywordSearchRequest request) {
        boardReader.validateReadable(memberId, request.boardId());

        PostPageQueryResult queryResult = postReader.readKeywordPage(memberId, request);
        int page = request.pageOrDefault();
        int size = request.sizeOrDefault();
        long searchLimit = pageBlockSearchLimit(page, size);
        PageBlock pageBlock = resolvePageBlock(
                page, size, postReader.countPostsByKeywordUpTo(request, searchLimit));

        return new PostPageResponse(
                toResponses(queryResult.posts()),
                page,
                size,
                queryResult.hasNext(),
                pageBlock.lastPage(),
                pageBlock.hasNextBlock(),
                false
        );
    }

    public PostCursorPageResponse readCursor(Long memberId, PostCursorSearchRequest request) {
        boardReader.validateReadable(memberId, request.boardId());

        PostPageQueryResult queryResult = postReader.readCursorPage(memberId, request);
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

    private List<PostSummaryResponse> toResponses(List<PostSummaryQueryDto> posts) {
        return posts.stream().map(PostSummaryResponse::from).toList();
    }

    /**
     * 전체 개수가 아니라 현재 페이지 블록과 다음 블록 존재 여부를 판단할 만큼만 센다.
     */
    private long pageBlockSearchLimit(int page, int size) {
        int blockIndex = (page - 1) / PAGE_BLOCK_SIZE;
        return (long) (blockIndex + 1) * size * PAGE_BLOCK_SIZE + 1;
    }

    private PageBlock resolvePageBlock(int page, int size, long cappedCount) {
        int blockIndex = (page - 1) / PAGE_BLOCK_SIZE;
        if (cappedCount >= pageBlockSearchLimit(page, size)) {
            return new PageBlock((blockIndex + 1) * PAGE_BLOCK_SIZE, true);
        }
        int lastPage = (int) Math.max(1, (cappedCount + size - 1) / size);
        return new PageBlock(lastPage, false);
    }

    private record PageBlock(int lastPage, boolean hasNextBlock) {
    }
}
