package cluverse.post.controller;

import cluverse.common.api.response.ApiResponse;
import cluverse.common.auth.Login;
import cluverse.common.auth.LoginMember;
import cluverse.post.service.PostListQueryServiceV4;
import cluverse.post.service.request.PostCursorSearchRequest;
import cluverse.post.service.response.PostCursorPageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v4/posts")
@RequiredArgsConstructor
public class PostControllerV4 {

    private final PostListQueryServiceV4 postListQueryServiceV4;

    @GetMapping
    public ApiResponse<PostCursorPageResponse> getPostList(@Login LoginMember loginMember,
                                                           @Valid @ModelAttribute PostCursorSearchRequest request) {
        Long memberId = loginMember == null ? null : loginMember.memberId();
        return ApiResponse.ok(postListQueryServiceV4.getPosts(memberId, request));
    }
}
