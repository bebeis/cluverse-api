package cluverse.post.client;

import java.util.UUID;

public record PostImageProcessCommand(
        UUID requestId,
        int displayOrder,
        String stagingKey,
        String contentKey,
        String thumbnailKey,
        String policyVersion
) {
}
