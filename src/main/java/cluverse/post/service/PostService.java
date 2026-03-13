package cluverse.post.service;

import cluverse.post.service.request.PostCreateRequest;
import cluverse.post.service.request.PostSearchRequest;
import cluverse.post.service.request.PostUpdateRequest;
import cluverse.post.service.response.PostDetailResponse;
import cluverse.post.service.response.PostPageResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PostService {

    @Transactional(readOnly = true)
    public PostPageResponse getPosts(Long memberId, PostSearchRequest request) {
        throw unsupported();
    }

    public PostDetailResponse createPost(Long memberId, PostCreateRequest request, String clientIp) {
        throw unsupported();
    }

    @Transactional(readOnly = true)
    public PostDetailResponse readPost(Long memberId, Long postId) {
        throw unsupported();
    }

    public PostDetailResponse updatePost(Long memberId, Long postId, PostUpdateRequest request) {
        throw unsupported();
    }

    public void deletePost(Long memberId, Long postId) {
        throw unsupported();
    }

    public void increaseLikeCount(Long postId) {
        throw unsupported();
    }

    public void decreaseLikeCount(Long postId) {
        throw unsupported();
    }

    public void increaseBookmarkCount(Long postId) {
        throw unsupported();
    }

    public void decreaseBookmarkCount(Long postId) {
        throw unsupported();
    }

    private UnsupportedOperationException unsupported() {
        return new UnsupportedOperationException("게시글 서비스는 아직 구현되지 않았습니다.");
    }
}
