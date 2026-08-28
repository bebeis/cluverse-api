package cluverse.post.client;

import cluverse.post.domain.PostImageProcessingPlan;

import java.util.UUID;

public record PostImageProcessCommand(
        UUID requestId,
        int displayOrder,
        String stagingKey,
        String contentKey,
        String thumbnailKey,
        String policyVersion
) {
    public static PostImageProcessCommand from(PostImageProcessingPlan plan) {
        return new PostImageProcessCommand(
                plan.requestId(),
                plan.displayOrder(),
                plan.stagingKey(),
                plan.contentKey(),
                plan.thumbnailKey(),
                plan.policyVersion()
        );
    }
}
