package cluverse.post.domain;

import java.util.UUID;

public record PostImageProcessingPlan(
        UUID requestId,
        int displayOrder,
        String stagingKey,
        String contentKey,
        String thumbnailKey,
        String policyVersion
) {
}
