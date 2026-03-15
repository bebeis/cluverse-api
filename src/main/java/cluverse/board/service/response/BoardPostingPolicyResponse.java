package cluverse.board.service.response;

import cluverse.post.domain.PostCategory;

import java.util.List;

public record BoardPostingPolicyResponse(
        boolean isAnonymousAllowed,
        boolean isExternalVisibleAllowed,
        boolean isPinnedAllowed,
        String externalVisibilityRule,
        String writePermissionRule,
        List<PostCategory> supportedCategories
) {
    public BoardPostingPolicyResponse {
        supportedCategories = supportedCategories == null ? List.of() : List.copyOf(supportedCategories);
    }
}
