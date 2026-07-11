package cluverse.post.service.implement;

import cluverse.post.repository.PostQueryRepository;
import cluverse.post.repository.dto.PostDetailQueryDto;
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

    private final PostQueryRepository postQueryRepository;

    public PostPageQueryResult readPostPage(Long memberId, PostSearchRequest request) {
        return postQueryRepository.findPostPage(memberId, request);
    }

    public PostPageQueryResult readPostPageByDate(Long memberId, PostSearchRequest request) {
        return postQueryRepository.findPostPageByDate(memberId, request);
    }

    public PostPageQueryResult readPostPageByKeyword(Long memberId, PostKeywordSearchRequest request) {
        return postQueryRepository.findPostPageByKeyword(memberId, request);
    }

    public PostPageQueryResult readPostPageByAuthor(Long viewerId, Long authorId, int page, int size) {
        return postQueryRepository.findPostPageByAuthor(viewerId, authorId, page, size);
    }

    public PostDetailQueryDto readPostDetail(Long memberId, Long postId) {
        return postQueryRepository.findPostDetail(memberId, postId);
    }
}
