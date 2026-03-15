package cluverse.group.service.response;

import cluverse.group.domain.GroupMemberRole;

import java.time.LocalDateTime;

public record GroupMemberResponse(
        Long memberId,
        String nickname,
        String profileImageUrl,
        GroupMemberRole role,
        Long customTitleId,
        String customTitle,
        LocalDateTime joinedAt,
        boolean isMe
) {
}
