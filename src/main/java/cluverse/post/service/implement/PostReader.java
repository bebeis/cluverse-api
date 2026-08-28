package cluverse.post.service.implement;

import cluverse.common.exception.NotFoundException;
import cluverse.post.exception.PostExceptionMessage;
import cluverse.post.repository.PostPageQueryRepository;
import cluverse.post.repository.PostQueryRepository;
import cluverse.post.repository.dto.PostDetailQueryDto;
import cluverse.post.repository.dto.PostIdSliceQueryResult;
import cluverse.post.repository.dto.PostPageQueryResult;
import cluverse.post.repository.dto.PostSummaryQueryDto;
import cluverse.post.service.request.PostCursorSearchRequest;
import cluverse.post.service.request.PostKeywordSearchRequest;
import cluverse.post.service.request.PostPageSearchRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostReader {

    private final PostPageQueryRepository postPageQueryRepository;
    private final PostQueryRepository postQueryRepository;

    public List<PostSummaryQueryDto> readPostSummaries(
            Long memberId,
            List<Long> postIds
    ) {
        return postQueryRepository.findPostSummaries(memberId, postIds);
    }

    public PostPageQueryResult readCursorPage(Long memberId, PostCursorSearchRequest request) {
        return toPageResult(memberId, postPageQueryRepository.findCursorPageIds(request));
    }

    public PostPageQueryResult readOffsetPage(Long memberId, PostPageSearchRequest request) {
        return toPageResult(memberId, postPageQueryRepository.findOffsetPageIds(request));
    }

    public boolean existsPostsNewerThan(PostCursorSearchRequest request) {
        return postPageQueryRepository.existsPostsNewerThan(
                request.boardId(), request.category(), request.exclusiveDateEnd());
    }

    public PostPageQueryResult readKeywordPage(Long memberId, PostKeywordSearchRequest request) {
        return toPageResult(memberId, postPageQueryRepository.findKeywordPageIds(request));
    }

    public PostPageQueryResult readAuthorPage(Long viewerId, Long authorId, int page, int size) {
        return toPageResult(viewerId, postPageQueryRepository.findAuthorPageIds(authorId, page, size));
    }

    public long countPostsByKeywordUpTo(PostKeywordSearchRequest request, long searchLimit) {
        return postPageQueryRepository.countPostsByKeywordUpTo(request, searchLimit);
    }

    public long countPostsUpTo(PostPageSearchRequest request, long searchLimit) {
        return postPageQueryRepository.countPostsUpTo(request, searchLimit);
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
