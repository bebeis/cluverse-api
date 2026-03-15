package cluverse.reaction.controller;

import cluverse.common.api.response.ApiResponse;
import cluverse.common.auth.Login;
import cluverse.common.auth.LoginMember;
import cluverse.reaction.service.PostReactionService;
import cluverse.reaction.service.response.PostBookmarkResponse;
import cluverse.reaction.service.response.PostLikeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostReactionController {

    private final PostReactionService postReactionService;

    @PostMapping("/{postId}/likes")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PostLikeResponse> likePost(@Login LoginMember loginMember,
                                                  @PathVariable Long postId) {
        return ApiResponse.created(postReactionService.likePost(loginMember.memberId(), postId));
    }

    @DeleteMapping("/{postId}/likes")
    public ApiResponse<PostLikeResponse> unlikePost(@Login LoginMember loginMember,
                                                    @PathVariable Long postId) {
        return ApiResponse.ok(postReactionService.unlikePost(loginMember.memberId(), postId));
    }

    @PostMapping("/{postId}/bookmarks")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PostBookmarkResponse> bookmarkPost(@Login LoginMember loginMember,
                                                          @PathVariable Long postId) {
        return ApiResponse.created(postReactionService.bookmarkPost(loginMember.memberId(), postId));
    }

    @DeleteMapping("/{postId}/bookmarks")
    public ApiResponse<PostBookmarkResponse> removeBookmark(@Login LoginMember loginMember,
                                                            @PathVariable Long postId) {
        return ApiResponse.ok(postReactionService.removeBookmark(loginMember.memberId(), postId));
    }
}
