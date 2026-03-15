package cluverse.recruitment.service.response;

import cluverse.recruitment.domain.Recruitment;
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

    public static RecruitmentSummaryResponse from(Recruitment recruitment) {
        return new RecruitmentSummaryResponse(
                recruitment.getId(),
                recruitment.getGroupId(),
                recruitment.getTitle(),
                recruitment.getPositions().stream()
                        .map(RecruitmentPositionResponse::from)
                        .toList(),
                recruitment.getDeadline(),
                recruitment.getStatus(),
                recruitment.getApplicationCount(),
                recruitment.getCreatedAt()
        );
    }
}
