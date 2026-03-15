package cluverse.board.service.response;

import cluverse.post.domain.PostCategory;

public record BoardHomeTabResponse(
        BoardHomeTabType tab,
        String label,
        PostCategory category,
        boolean isDefault,
        boolean isVisible,
        boolean isWriteAllowed
) {
}
