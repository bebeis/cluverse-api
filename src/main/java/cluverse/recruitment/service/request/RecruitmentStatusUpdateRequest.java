package cluverse.recruitment.service.request;

import cluverse.recruitment.domain.RecruitmentStatus;
import jakarta.validation.constraints.NotNull;

public record RecruitmentStatusUpdateRequest(
        @NotNull(message = "모집 상태를 입력해주세요.")
        RecruitmentStatus status
) {
}
