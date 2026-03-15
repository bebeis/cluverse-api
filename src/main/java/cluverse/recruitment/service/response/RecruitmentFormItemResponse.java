package cluverse.recruitment.service.response;

import cluverse.recruitment.domain.FormItem;
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

    public static RecruitmentFormItemResponse from(FormItem formItem) {
        return new RecruitmentFormItemResponse(
                formItem.getId(),
                formItem.getQuestion(),
                formItem.getQuestionType(),
                formItem.isRequired(),
                formItem.getOptions(),
                formItem.getDisplayOrder()
        );
    }
}
