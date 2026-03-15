package cluverse.reaction.controller;

import cluverse.common.api.response.ApiResponse;
import cluverse.common.auth.Login;
import cluverse.common.auth.LoginMember;
import cluverse.reaction.service.CommentReactionService;
import cluverse.reaction.service.response.CommentLikeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/comments")
@RequiredArgsConstructor
public class CommentReactionController {

    private final CommentReactionService commentReactionService;

    @PostMapping("/{commentId}/likes")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CommentLikeResponse> likeComment(@Login LoginMember loginMember,
                                                        @PathVariable Long commentId) {
        return ApiResponse.created(commentReactionService.likeComment(loginMember.memberId(), commentId));
    }

    @DeleteMapping("/{commentId}/likes")
    public ApiResponse<CommentLikeResponse> unlikeComment(@Login LoginMember loginMember,
                                                          @PathVariable Long commentId) {
        return ApiResponse.ok(commentReactionService.unlikeComment(loginMember.memberId(), commentId));
    }
}
