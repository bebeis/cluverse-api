package cluverse.member.service.response;

import java.time.LocalDateTime;

public record MemberProfileImagePresignedUrlResponse(
        String fileKey,
        String uploadUrl,
        String imageUrl,
        LocalDateTime expiresAt
) {
}
