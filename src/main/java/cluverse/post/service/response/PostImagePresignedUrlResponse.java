package cluverse.post.service.response;

import java.time.LocalDateTime;

public record PostImagePresignedUrlResponse(
        String fileKey,
        String uploadUrl,
        String imageUrl,
        LocalDateTime expiresAt
) {
}
