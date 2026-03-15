package cluverse.recruitment.service.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RecruitmentApplicationAnswerRequest(
        @NotNull(message = "질문 항목 ID를 입력해주세요.")
        Long formItemId,

        @NotBlank(message = "답변을 입력해주세요.")
        String answer
) {
}
