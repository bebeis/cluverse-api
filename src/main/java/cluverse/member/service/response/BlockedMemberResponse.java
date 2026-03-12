package cluverse.member.service.response;

import java.time.LocalDateTime;

public record BlockedMemberResponse(
        Long memberId,
        String nickname,
        Long universityId,
        String universityName,
        String universityBadgeImageUrl,
        String profileImageUrl,
        LocalDateTime blockedAt
) {
}
