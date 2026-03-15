package cluverse.recruitment.service.response;

import cluverse.recruitment.domain.RecruitmentStatus;

import java.time.LocalDateTime;
import java.util.List;

public record RecruitmentDetailResponse(
        Long recruitmentId,
        Long groupId,
        Long authorId,
        String authorNickname,
        String title,
        String description,
        List<RecruitmentPositionResponse> positions,
        String requirements,
        String duration,
        String goal,
        String processDescription,
        LocalDateTime deadline,
        RecruitmentStatus status,
        int applicationCount,
        List<RecruitmentFormItemResponse> formItems,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public RecruitmentDetailResponse {
        positions = positions == null ? List.of() : List.copyOf(positions);
        formItems = formItems == null ? List.of() : List.copyOf(formItems);
    }
}
