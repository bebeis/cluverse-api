package cluverse.recruitment.service.request;

import cluverse.recruitment.domain.RecruitmentApplicationStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RecruitmentApplicationStatusUpdateRequest(
        @NotNull(message = "지원 상태를 입력해주세요.")
        RecruitmentApplicationStatus status,

        @Size(max = 1000, message = "메모는 1000자 이하여야 합니다.")
        String note
) {
}
