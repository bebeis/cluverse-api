package cluverse.post.service;

import cluverse.post.service.request.PostCreateRequest;
import cluverse.post.service.request.PostKeywordSearchRequest;
import cluverse.post.service.request.PostSearchRequest;
import cluverse.post.service.request.PostUpdateRequest;
import cluverse.post.service.response.PostDetailResponse;
import cluverse.post.service.response.PostPageResponse;
import cluverse.post.service.response.PostTitleResponse;

import java.util.List;

public interface PostService {

    PostPageResponse getPosts(Long memberId, PostSearchRequest request);

    PostPageResponse searchPosts(Long memberId, PostKeywordSearchRequest request);

    PostDetailResponse createPost(Long memberId, PostCreateRequest request, String clientIp);

    PostDetailResponse readPost(Long memberId, Long postId);

    void increaseViewCount(Long postId);

    PostDetailResponse updatePost(Long memberId, Long postId, PostUpdateRequest request);

    void deletePost(Long memberId, Long postId);

    void validatePostExists(Long postId);

    void validateReadablePost(Long memberId, Long postId);

    void validateWritablePost(Long memberId, Long postId);

    List<PostTitleResponse> getRecentCommentRepliedPosts(Long size);
}
