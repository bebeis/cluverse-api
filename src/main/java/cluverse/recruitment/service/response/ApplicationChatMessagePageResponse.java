package cluverse.recruitment.service.response;

import java.util.List;

public record ApplicationChatMessagePageResponse(
        List<ApplicationChatMessageResponse> messages,
        Long beforeMessageId,
        int limit,
        boolean hasNext
) {
    public ApplicationChatMessagePageResponse {
        messages = messages == null ? List.of() : List.copyOf(messages);
    }
}
