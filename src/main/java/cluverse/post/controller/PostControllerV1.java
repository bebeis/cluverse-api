package cluverse.post.controller;

import cluverse.common.api.response.ApiResponse;
import cluverse.common.auth.Login;
import cluverse.common.auth.LoginMember;
import cluverse.post.service.PostQueryService;
import cluverse.post.service.PostService;
import cluverse.post.service.request.PostCreateRequest;
import cluverse.post.service.request.PostKeywordSearchRequest;
import cluverse.post.service.request.PostSearchRequest;
import cluverse.post.service.request.PostUpdateRequest;
import cluverse.post.service.response.PostDetailResponse;
import cluverse.post.service.response.PostPageResponse;
import cluverse.post.service.response.PostTitleResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostControllerV1 {

    private final PostQueryService postQueryService;
    private final PostService postService;

    @GetMapping
    public ApiResponse<PostPageResponse> getPostList(@Login LoginMember loginMember,
                                                     @Valid @ModelAttribute PostSearchRequest request) {
        return ApiResponse.ok(postQueryService.getPosts(extractMemberId(loginMember), request));
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
