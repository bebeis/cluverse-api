package cluverse.post.controller;

import cluverse.common.api.response.ApiResponse;
import cluverse.common.auth.Login;
import cluverse.common.auth.LoginMember;
import cluverse.post.service.PostListQueryService;
import cluverse.post.service.PostQueryService;
import cluverse.post.service.PostService;
import cluverse.post.service.request.PostCreateRequest;
import cluverse.post.service.request.PostKeywordSearchRequest;
import cluverse.post.service.request.PostCursorSearchRequest;
import cluverse.post.service.request.PostPageSearchRequest;
import cluverse.post.service.request.PostUpdateRequest;
import cluverse.post.service.response.PostDetailResponse;
import cluverse.post.service.response.PostCursorPageResponse;
import cluverse.post.service.response.PostPageResponse;
import cluverse.post.service.response.PostTitleResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
public class PostController {

    private final PostQueryService postQueryService;
    private final PostListQueryService postListQueryService;
    private final PostService postService;
    private final ViewCountCookieResolver viewCountCookieResolver;

    @GetMapping(params = {"!cursorCreatedAt", "!cursorPostId", "!date"})
    public ApiResponse<PostPageResponse> getPostList(
            @Login LoginMember loginMember,
            @Valid @ModelAttribute PostPageSearchRequest request
    ) {
        return ApiResponse.ok(postListQueryService.readPage(extractMemberId(loginMember), request));
    }

    @GetMapping(params = {"cursorCreatedAt", "cursorPostId"})
    public ApiResponse<PostCursorPageResponse> getPostListByCursor(
            @Login LoginMember loginMember,
            @Valid @ModelAttribute PostCursorSearchRequest request
    ) {
        return ApiResponse.ok(postListQueryService.readCursor(extractMemberId(loginMember), request));
    }

    @GetMapping(params = {"date", "!cursorCreatedAt", "!cursorPostId"})
    public ApiResponse<PostCursorPageResponse> getPostListByDate(
            @Login LoginMember loginMember,
            @Valid @ModelAttribute PostCursorSearchRequest request
    ) {
        return ApiResponse.ok(postListQueryService.readCursor(extractMemberId(loginMember), request));
    }

    @GetMapping("/cursor")
    public ApiResponse<PostCursorPageResponse> getPostListByCursorPath(
            @Login LoginMember loginMember,
            @Valid @ModelAttribute PostCursorSearchRequest request
    ) {
        return ApiResponse.ok(postListQueryService.readCursor(extractMemberId(loginMember), request));
    }

    @GetMapping("/search")
    public ApiResponse<PostPageResponse> searchPosts(@Login LoginMember loginMember,
                                                     @Valid @ModelAttribute PostKeywordSearchRequest request) {
        return ApiResponse.ok(postListQueryService.search(extractMemberId(loginMember), request));
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
                                                    @PathVariable Long postId,
                                                    HttpServletRequest request,
                                                    HttpServletResponse response) {
        Long memberId = extractMemberId(loginMember);
        String cookieId = viewCountCookieResolver.resolve(request, response);
        long currentViewCount = postService.countView(memberId, postId, cookieId).viewCount();
        return ApiResponse.ok(postQueryService.readPost(memberId, postId).withViewCount(currentViewCount));
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
