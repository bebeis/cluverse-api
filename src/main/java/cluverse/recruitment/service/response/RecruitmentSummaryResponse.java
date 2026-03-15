package cluverse.recruitment.service.response;

import cluverse.recruitment.domain.RecruitmentStatus;

import java.time.LocalDateTime;
import java.util.List;

public record RecruitmentSummaryResponse(
        Long recruitmentId,
        Long groupId,
        String title,
        List<RecruitmentPositionResponse> positions,
        LocalDateTime deadline,
        RecruitmentStatus status,
        int applicationCount,
        LocalDateTime createdAt
) {
    public RecruitmentSummaryResponse {
        positions = positions == null ? List.of() : List.copyOf(positions);
    }
}
