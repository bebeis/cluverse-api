package cluverse.post.controller;

import cluverse.common.api.response.ApiResponse;
import cluverse.common.auth.Login;
import cluverse.common.auth.LoginMember;
import cluverse.post.service.PostService;
import cluverse.post.service.request.PostCreateRequest;
import cluverse.post.service.request.PostSearchRequest;
import cluverse.post.service.request.PostUpdateRequest;
import cluverse.post.service.response.PostDetailResponse;
import cluverse.post.service.response.PostPageResponse;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @GetMapping
    public ApiResponse<PostPageResponse> getPostList(@Login LoginMember loginMember,
                                                     @Valid @ModelAttribute PostSearchRequest request) {
        return ApiResponse.ok(postService.getPosts(loginMember.memberId(), request));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PostDetailResponse> createPost(@Login LoginMember loginMember,
                                                      @RequestBody @Valid PostCreateRequest request,
                                                      HttpServletRequest httpRequest) {
        return ApiResponse.created(
                postService.createPost(loginMember.memberId(), request, httpRequest.getRemoteAddr())
        );
    }

    @GetMapping("/{postId}")
    public ApiResponse<PostDetailResponse> readPost(@Login LoginMember loginMember,
                                                    @PathVariable Long postId) {
        return ApiResponse.ok(postService.readPost(loginMember.memberId(), postId));
    }

    @PutMapping("/{postId}")
    public ApiResponse<PostDetailResponse> updatePost(@Login LoginMember loginMember,
                                                      @PathVariable Long postId,
                                                      @RequestBody @Valid PostUpdateRequest request) {
        return ApiResponse.ok(postService.updatePost(loginMember.memberId(), postId, request));
    }

    @DeleteMapping("/{postId}")
    public ApiResponse<Void> deletePost(@Login LoginMember loginMember,
                                        @PathVariable Long postId) {
        postService.deletePost(loginMember.memberId(), postId);
        return ApiResponse.ok();
    }
}
