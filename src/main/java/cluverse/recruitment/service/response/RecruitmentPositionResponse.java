package cluverse.recruitment.service.response;

import cluverse.recruitment.domain.RecruitmentPosition;

public record RecruitmentPositionResponse(
        String name,
        int count
) {
    public static RecruitmentPositionResponse from(RecruitmentPosition position) {
        return new RecruitmentPositionResponse(position.name(), position.count());
    }
}
