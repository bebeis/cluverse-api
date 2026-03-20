package cluverse.recruitment.repository.dto;

import java.time.LocalDateTime;

public record ApplicationChatMessageQueryDto(
        Long applicationChatMessageId,
        Long applicationId,
        Long senderId,
        String senderNickname,
        String senderProfileImageUrl,
        String content,
        boolean isRead,
        LocalDateTime createdAt
) {
}
