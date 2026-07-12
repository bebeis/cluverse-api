package cluverse.post.service;

import cluverse.board.service.implement.BoardReader;
import cluverse.post.repository.dto.PostPageQueryResult;
import cluverse.post.service.implement.PostReader;
import cluverse.post.service.request.PostOffsetSearchRequest;
import cluverse.post.service.response.PostPageResponse;
import cluverse.post.service.response.PostSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * [V2] 커버링 인덱스 deferred join 목록 조회.
 * id만 커버링 인덱스로 선정한 뒤 선정된 id에 대해서만 프로젝션한다.
 * 카운트는 아직 전체 COUNT(*) — 카운트 개선은 V3에서.
 */
@Service
@RequiredArgsConstructor
public class PostListQueryServiceV2 {

    private static final int PAGE_BLOCK_SIZE = 10;

    private final PostReader postReader;
    private final BoardReader boardReader;

    public PostPageResponse getPosts(Long memberId, PostOffsetSearchRequest request) {
        boardReader.validateReadable(memberId, request.boardId());

        PostPageQueryResult queryResult = postReader.readPostPage(memberId, request);
        List<PostSummaryResponse> responses = queryResult.posts().stream()
                .map(PostSummaryResponse::from)
                .toList();

        long totalCount = postReader.countPosts(request);
        int page = request.pageOrDefault();
        int size = request.sizeOrDefault();

        int actualLastPage = (int) Math.max(1, (totalCount + size - 1) / size);
        int blockEndPage = ((page - 1) / PAGE_BLOCK_SIZE + 1) * PAGE_BLOCK_SIZE;
        boolean hasNextBlock = actualLastPage > blockEndPage;

        return new PostPageResponse(
                responses,
                page,
                size,
                queryResult.hasNext(),
                hasNextBlock ? blockEndPage : actualLastPage,
                hasNextBlock,
                false
        );
    }
}
