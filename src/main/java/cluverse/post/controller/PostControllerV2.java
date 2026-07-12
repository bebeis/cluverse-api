package cluverse.post.controller;

import cluverse.common.api.response.ApiResponse;
import cluverse.common.auth.Login;
import cluverse.common.auth.LoginMember;
import cluverse.post.service.PostListQueryServiceV2;
import cluverse.post.service.request.PostOffsetSearchRequest;
import cluverse.post.service.response.PostPageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * [V2] 커버링 인덱스 deferred join 목록 조회.
 * 목록 조회 성능 개선 단계를 버전별로 비교 측정하기 위한 엔드포인트로,
 * 목록 조회 외의 게시글 API는 /api/v1에만 있다.
 */
@RestController
@RequestMapping("/api/v2/posts")
@RequiredArgsConstructor
public class PostControllerV2 {

    private final PostListQueryServiceV2 postListQueryServiceV2;

    @GetMapping
    public ApiResponse<PostPageResponse> getPostList(@Login LoginMember loginMember,
                                                     @Valid @ModelAttribute PostOffsetSearchRequest request) {
        Long memberId = loginMember == null ? null : loginMember.memberId();
        return ApiResponse.ok(postListQueryServiceV2.getPosts(memberId, request));
    }
}
