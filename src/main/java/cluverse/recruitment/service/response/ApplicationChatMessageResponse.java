package cluverse.recruitment.service.response;

import java.time.LocalDateTime;

public record ApplicationChatMessageResponse(
        Long applicationChatMessageId,
        Long applicationId,
        Long senderId,
        String senderNickname,
        String senderProfileImageUrl,
        String content,
        boolean isMine,
        boolean isRead,
        LocalDateTime createdAt
) {
}
