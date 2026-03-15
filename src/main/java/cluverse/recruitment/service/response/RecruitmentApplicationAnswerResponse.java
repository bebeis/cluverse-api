package cluverse.recruitment.service.response;

import cluverse.recruitment.domain.FormItemAnswer;

public record RecruitmentApplicationAnswerResponse(
        Long formItemId,
        String question,
        String answer
) {
    public static RecruitmentApplicationAnswerResponse of(FormItemAnswer answer, String question) {
        return new RecruitmentApplicationAnswerResponse(answer.getFormItemId(), question, answer.getAnswer());
    }
}
