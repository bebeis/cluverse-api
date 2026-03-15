package cluverse.recruitment.service.request;

import cluverse.recruitment.domain.FormItemQuestionType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record RecruitmentFormItemRequest(
        @NotBlank(message = "질문을 입력해주세요.")
        @Size(max = 500, message = "질문은 500자 이하여야 합니다.")
        String question,

        @NotNull(message = "질문 유형을 선택해주세요.")
        FormItemQuestionType questionType,

        boolean isRequired,

        @Size(max = 20, message = "선택지는 최대 20개까지 입력할 수 있습니다.")
        List<
                @NotBlank(message = "선택지는 비어 있을 수 없습니다.")
                @Size(max = 100, message = "선택지는 100자 이하여야 합니다.")
                        String> options,

        @NotNull(message = "질문 순서를 입력해주세요.")
        @Min(value = 1, message = "질문 순서는 1 이상이어야 합니다.")
        @Max(value = 5, message = "질문 순서는 5 이하여야 합니다.")
        Integer displayOrder
) {
    public RecruitmentFormItemRequest {
        options = options == null ? List.of() : List.copyOf(options);
    }
}
