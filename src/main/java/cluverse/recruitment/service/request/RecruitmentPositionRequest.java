package cluverse.recruitment.service.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RecruitmentPositionRequest(
        @NotBlank(message = "모집 포지션명을 입력해주세요.")
        @Size(max = 50, message = "모집 포지션명은 50자 이하여야 합니다.")
        String name,

        @Min(value = 1, message = "모집 인원은 1명 이상이어야 합니다.")
        Integer count
) {
}
