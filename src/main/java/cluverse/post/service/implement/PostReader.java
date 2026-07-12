package cluverse.post.service.implement;

import cluverse.common.exception.NotFoundException;
import cluverse.post.exception.PostExceptionMessage;
import cluverse.post.repository.PostPageQueryRepository;
import cluverse.post.repository.PostQueryRepository;
import cluverse.post.repository.dto.PostDetailQueryDto;
import cluverse.post.repository.dto.PostIdSliceQueryResult;
import cluverse.post.repository.dto.PostPageQueryResult;
import cluverse.post.service.request.PostCursorSearchRequest;
import cluverse.post.service.request.PostKeywordSearchRequest;
import cluverse.post.service.request.PostOffsetSearchRequest;
import cluverse.post.service.request.PostSearchRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostReader {

    private final PostPageQueryRepository postPageQueryRepository;
    private final PostQueryRepository postQueryRepository;

    public PostPageQueryResult readPostPage(Long memberId, PostSearchRequest request) {
        return toPageResult(memberId, postPageQueryRepository.findPostPageIds(request));
    }

    /**
     * [V1 전용] naive offset 단일 쿼리 조회. hasNext는 전체 카운트로 서비스에서 계산한다.
     */
    public PostPageQueryResult readPostPageWithOffset(Long memberId, PostOffsetSearchRequest request) {
        return new PostPageQueryResult(
                postQueryRepository.findPostSummariesWithOffset(memberId, request),
                false
        );
    }

    /**
     * [V2 전용] 커버링 인덱스 id 선정 + 프로젝션 조회.
     */
    public PostPageQueryResult readPostPage(Long memberId, PostOffsetSearchRequest request) {
        return toPageResult(memberId, postPageQueryRepository.findPostPageIds(request));
    }

    /**
     * [V4 전용] 날짜 앵커/커서 기반 조회.
     */
    public PostPageQueryResult readPostPageByCursor(Long memberId, PostCursorSearchRequest request) {
        return toPageResult(memberId, postPageQueryRepository.findPostPageIdsByCursor(request));
    }

    /**
     * [V1/V2 전용] 전체 카운트.
     */
    public long countPosts(PostOffsetSearchRequest request) {
        return postPageQueryRepository.countPosts(request.boardId(), request.category());
    }

    /**
     * [V4 전용] date 진입 페이지의 hasPrev 판단.
     */
    public boolean existsPostsNewerThan(PostCursorSearchRequest request) {
        return postPageQueryRepository.existsPostsNewerThan(
                request.boardId(), request.category(), request.exclusiveDateEnd());
    }

    public PostPageQueryResult readPostPageByDate(Long memberId, PostSearchRequest request) {
        return toPageResult(memberId, postPageQueryRepository.findPostPageIdsByDate(request));
    }

    public PostPageQueryResult readPostPageByKeyword(Long memberId, PostKeywordSearchRequest request) {
        return toPageResult(memberId, postPageQueryRepository.findPostPageIdsByKeyword(request));
    }

    public PostPageQueryResult readPostPageByAuthor(Long viewerId, Long authorId, int page, int size) {
        return toPageResult(viewerId, postPageQueryRepository.findPostPageIdsByAuthor(authorId, page, size));
    }

    public long countPostsUpTo(PostSearchRequest request, long searchLimit) {
        return postPageQueryRepository.countPostsUpTo(request, searchLimit);
    }

    public long countPostsByKeywordUpTo(PostKeywordSearchRequest request, long searchLimit) {
        return postPageQueryRepository.countPostsByKeywordUpTo(request, searchLimit);
    }

    public PostDetailQueryDto readPostDetail(Long memberId, Long postId) {
        return postQueryRepository.findPostDetail(memberId, postId)
                .orElseThrow(() -> new NotFoundException(PostExceptionMessage.POST_NOT_FOUND.getMessage()));
    }

    private PostPageQueryResult toPageResult(Long memberId, PostIdSliceQueryResult slice) {
        return new PostPageQueryResult(
                postQueryRepository.findPostSummaries(memberId, slice.postIds()),
                slice.hasNext()
        );
    }
}
