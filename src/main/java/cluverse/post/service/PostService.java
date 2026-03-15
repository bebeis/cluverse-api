package cluverse.post.service;

import cluverse.post.service.request.PostCreateRequest;
import cluverse.post.service.request.PostSearchRequest;
import cluverse.post.service.request.PostUpdateRequest;
import cluverse.post.service.response.PostDetailResponse;
import cluverse.post.service.response.PostPageResponse;
public interface PostService {

    PostPageResponse getPosts(Long memberId, PostSearchRequest request);

    PostDetailResponse createPost(Long memberId, PostCreateRequest request, String clientIp);

    PostDetailResponse readPost(Long memberId, Long postId);

    void increaseViewCount(Long postId);

    PostDetailResponse updatePost(Long memberId, Long postId, PostUpdateRequest request);

    void deletePost(Long memberId, Long postId);

    void validatePostExists(Long postId);
}
