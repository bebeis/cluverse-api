package cluverse.recruitment.service.response;

import java.util.List;

public record RecruitmentPageResponse(
        List<RecruitmentSummaryResponse> recruitments,
        int page,
        int size,
        boolean hasNext
) {
    public RecruitmentPageResponse {
        recruitments = recruitments == null ? List.of() : List.copyOf(recruitments);
    }
}
