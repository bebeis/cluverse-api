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
 * [V1] 인덱스만 건 원본(naive offset) 목록 조회.
 * 단일 쿼리로 offset 지점까지 풀 프로젝션 + 전체 COUNT(*).
 * 성능 비교 기준(baseline)으로 보존한다.
 */
@Service
@RequiredArgsConstructor
public class PostListQueryServiceV1 {

    private static final int PAGE_BLOCK_SIZE = 10;

    private final PostReader postReader;
    private final BoardReader boardReader;

    public PostPageResponse getPosts(Long memberId, PostOffsetSearchRequest request) {
        boardReader.validateReadable(memberId, request.boardId());

        PostPageQueryResult queryResult = postReader.readPostPageWithOffset(memberId, request);
        List<PostSummaryResponse> responses = queryResult.posts().stream()
                .map(PostSummaryResponse::from)
                .toList();

        long totalCount = postReader.countPosts(request);
        int page = request.pageOrDefault();
        int size = request.sizeOrDefault();

        // V3(블록 상한 카운트)과 같은 응답 의미를 전체 카운트로부터 계산한다.
        int actualLastPage = (int) Math.max(1, (totalCount + size - 1) / size);
        int blockEndPage = ((page - 1) / PAGE_BLOCK_SIZE + 1) * PAGE_BLOCK_SIZE;
        boolean hasNextBlock = actualLastPage > blockEndPage;

        return new PostPageResponse(
                responses,
                page,
                size,
                (long) page * size < totalCount,
                hasNextBlock ? blockEndPage : actualLastPage,
                hasNextBlock,
                false
        );
    }
}
