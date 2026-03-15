package cluverse.recruitment.service.response;

public record RecruitmentApplicationAnswerResponse(
        Long formItemId,
        String question,
        String answer
) {
}
