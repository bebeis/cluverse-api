package cluverse.recruitment.service.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.util.List;

public record RecruitmentApplicationCreateRequest(
        @Size(max = 50, message = "지원 포지션은 50자 이하여야 합니다.")
        String position,

        @Size(max = 500, message = "포트폴리오 URL은 500자 이하여야 합니다.")
        String portfolioUrl,

        @Size(max = 5, message = "지원 답변은 최대 5개까지 입력할 수 있습니다.")
        List<@Valid RecruitmentApplicationAnswerRequest> answers
) {
    public RecruitmentApplicationCreateRequest {
        answers = answers == null ? List.of() : List.copyOf(answers);
    }
}
