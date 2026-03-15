package cluverse.recruitment.service.response;

import cluverse.recruitment.domain.FormItemQuestionType;

import java.util.List;

public record RecruitmentFormItemResponse(
        Long formItemId,
        String question,
        FormItemQuestionType questionType,
        boolean isRequired,
        List<String> options,
        int displayOrder
) {
    public RecruitmentFormItemResponse {
        options = options == null ? List.of() : List.copyOf(options);
    }
}
