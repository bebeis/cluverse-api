package cluverse.post.service.implement;

import cluverse.common.exception.NotFoundException;
import cluverse.post.exception.PostExceptionMessage;
import cluverse.post.repository.PostPageQueryRepository;
import cluverse.post.repository.PostQueryRepository;
import cluverse.post.repository.dto.PostDetailQueryDto;
import cluverse.post.repository.dto.PostIdSliceQueryResult;
import cluverse.post.repository.dto.PostPageQueryResult;
import cluverse.post.service.request.PostKeywordSearchRequest;
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
