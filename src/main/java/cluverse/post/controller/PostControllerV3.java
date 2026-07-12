package cluverse.post.controller;

import cluverse.common.api.response.ApiResponse;
import cluverse.common.auth.Login;
import cluverse.common.auth.LoginMember;
import cluverse.post.service.PostQueryService;
import cluverse.post.service.request.PostSearchRequest;
import cluverse.post.service.response.PostPageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * [V3] deferred join + 페이지 블록 상한 카운트 목록 조회.
 * 프로덕션 구현({@link PostQueryService#getPosts})에 그대로 위임한다.
 */
@RestController
@RequestMapping("/api/v3/posts")
@RequiredArgsConstructor
public class PostControllerV3 {

    private final PostQueryService postQueryService;

    @GetMapping
    public ApiResponse<PostPageResponse> getPostList(@Login LoginMember loginMember,
                                                     @Valid @ModelAttribute PostSearchRequest request) {
        Long memberId = loginMember == null ? null : loginMember.memberId();
        return ApiResponse.ok(postQueryService.getPosts(memberId, request));
    }
}
