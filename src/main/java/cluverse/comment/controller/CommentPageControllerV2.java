package cluverse.comment.controller;

import cluverse.comment.service.CommentQueryService;
import cluverse.comment.service.request.CommentPageRequest;
import cluverse.comment.service.response.CommentPageResponse;
import cluverse.common.api.response.ApiResponse;
import cluverse.common.auth.Login;
import cluverse.common.auth.LoginMember;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/comments")
@RequiredArgsConstructor
public class CommentPageControllerV2 {

    private final CommentQueryService commentQueryService;

    @GetMapping
    public ApiResponse<CommentPageResponse> getComments(@Login LoginMember loginMember,
                                                        @Valid @ModelAttribute CommentPageRequest request) {
        return ApiResponse.ok(commentQueryService.getCommentsV2(extractMemberId(loginMember), request));
    }

    private Long extractMemberId(LoginMember loginMember) {
        return loginMember == null ? null : loginMember.memberId();
    }
}
