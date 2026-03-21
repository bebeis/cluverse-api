package cluverse.comment.controller;

import cluverse.comment.service.CommentQueryService;
import cluverse.comment.service.CommentService;
import cluverse.comment.service.request.CommentCreateRequest;
import cluverse.comment.service.request.CommentPageRequest;
import cluverse.comment.service.request.CommentUpdateRequest;
import cluverse.comment.service.response.CommentDeleteResponse;
import cluverse.comment.service.response.CommentPageResponse;
import cluverse.comment.service.response.CommentResponse;
import cluverse.common.api.response.ApiResponse;
import cluverse.common.auth.Login;
import cluverse.common.auth.LoginMember;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentQueryService commentQueryService;
    private final CommentService commentService;

    @GetMapping
    public ApiResponse<CommentPageResponse> getComments(@Login LoginMember loginMember,
                                                        @Valid @ModelAttribute CommentPageRequest request) {
        return ApiResponse.ok(commentQueryService.getComments(extractMemberId(loginMember), request));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CommentResponse> createComment(@Login LoginMember loginMember,
                                                      @RequestParam Long postId,
                                                      @RequestBody @Valid CommentCreateRequest request,
                                                      HttpServletRequest httpRequest) {
        return ApiResponse.created(
                commentService.createComment(loginMember.memberId(), postId, request, httpRequest.getRemoteAddr())
        );
    }

    @PutMapping("/{commentId}")
    public ApiResponse<CommentResponse> updateComment(@Login LoginMember loginMember,
                                                      @PathVariable Long commentId,
                                                      @RequestBody @Valid CommentUpdateRequest request) {
        return ApiResponse.ok(commentService.updateComment(loginMember.memberId(), commentId, request));
    }

    @DeleteMapping("/{commentId}")
    public ApiResponse<CommentDeleteResponse> deleteComment(@Login LoginMember loginMember,
                                                            @PathVariable Long commentId) {
        return ApiResponse.ok(commentService.deleteComment(loginMember.memberId(), commentId));
    }

    private Long extractMemberId(LoginMember loginMember) {
        return loginMember == null ? null : loginMember.memberId();
    }
}
