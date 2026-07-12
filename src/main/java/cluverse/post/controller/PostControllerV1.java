package cluverse.post.controller;

import cluverse.common.api.response.ApiResponse;
import cluverse.common.auth.Login;
import cluverse.common.auth.LoginMember;
import cluverse.post.service.PostListQueryServiceV1;
import cluverse.post.service.PostQueryService;
import cluverse.post.service.PostService;
import cluverse.post.service.request.PostCreateRequest;
import cluverse.post.service.request.PostKeywordSearchRequest;
import cluverse.post.service.request.PostOffsetSearchRequest;
import cluverse.post.service.request.PostUpdateRequest;
import cluverse.post.service.response.PostDetailResponse;
import cluverse.post.service.response.PostPageResponse;
import cluverse.post.service.response.PostTitleResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostControllerV1 {

    private final PostQueryService postQueryService;
    private final PostListQueryServiceV1 postListQueryServiceV1;
    private final PostService postService;

    /**
     * [V1] 인덱스만 건 원본(naive offset) 목록 조회. 성능 비교 기준(baseline).
     * 개선판은 /api/v2(deferred join), /api/v3(+ 블록 카운트), /api/v4(커서) 참조.
     */
    @GetMapping
    public ApiResponse<PostPageResponse> getPostList(@Login LoginMember loginMember,
                                                     @Valid @ModelAttribute PostOffsetSearchRequest request) {
        return ApiResponse.ok(postListQueryServiceV1.getPosts(extractMemberId(loginMember), request));
    }

    @GetMapping("/search")
    public ApiResponse<PostPageResponse> searchPosts(@Login LoginMember loginMember,
                                                     @Valid @ModelAttribute PostKeywordSearchRequest request) {
        return ApiResponse.ok(postQueryService.searchPosts(extractMemberId(loginMember), request));
    }

    @GetMapping("/recent-comment-replied")
    public ApiResponse<List<PostTitleResponse>> getRecentCommentRepliedPosts(@Login LoginMember loginMember,
                                                                             @RequestParam(required = false) Long size) {
        List<PostTitleResponse> postTitleResponses = postQueryService.getRecentCommentRepliedPosts(size);
        return ApiResponse.ok(postTitleResponses);
    }


    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PostDetailResponse> createPost(@Login LoginMember loginMember,
                                                      @RequestBody @Valid PostCreateRequest request,
                                                      HttpServletRequest httpRequest) {
        Long postId = postService.createPost(loginMember.memberId(), request, httpRequest.getRemoteAddr());
        return ApiResponse.created(
                postQueryService.readPost(loginMember.memberId(), postId)
        );
    }

    @GetMapping("/{postId}")
    public ApiResponse<PostDetailResponse> readPost(@Login LoginMember loginMember,
                                                    @PathVariable Long postId) {
        Long memberId = extractMemberId(loginMember);
        postService.increaseViewCount(memberId, postId);
        return ApiResponse.ok(postQueryService.readPost(memberId, postId));
    }

    @PutMapping("/{postId}")
    public ApiResponse<PostDetailResponse> updatePost(@Login LoginMember loginMember,
                                                      @PathVariable Long postId,
                                                      @RequestBody @Valid PostUpdateRequest request) {
        Long updatedPostId = postService.updatePost(loginMember.memberId(), postId, request);
        return ApiResponse.ok(postQueryService.readPost(loginMember.memberId(), updatedPostId));
    }

    @DeleteMapping("/{postId}")
    public ApiResponse<Void> deletePost(@Login LoginMember loginMember,
                                        @PathVariable Long postId) {
        postService.deletePost(loginMember.memberId(), postId);
        return ApiResponse.ok();
    }

    private Long extractMemberId(LoginMember loginMember) {
        return loginMember == null ? null : loginMember.memberId();
    }
}
