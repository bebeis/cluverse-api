package cluverse.recruitment.service.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ApplicationChatMessageCreateRequest(
        @NotBlank(message = "메시지를 입력해주세요.")
        @Size(max = 5000, message = "메시지는 5000자 이하여야 합니다.")
        String content
) {
}
