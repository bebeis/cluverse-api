package cluverse.recruitment.service.response;

import java.util.List;

public record RecruitmentApplicationPageResponse(
        List<RecruitmentApplicationSummaryResponse> applications,
        int page,
        int size,
        boolean hasNext
) {
    public RecruitmentApplicationPageResponse {
        applications = applications == null ? List.of() : List.copyOf(applications);
    }
}
