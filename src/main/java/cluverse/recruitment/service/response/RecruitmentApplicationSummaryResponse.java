package cluverse.recruitment.service.response;

import cluverse.recruitment.domain.RecruitmentApplicationStatus;

import java.time.LocalDateTime;

public record RecruitmentApplicationSummaryResponse(
        Long applicationId,
        Long recruitmentId,
        Long groupId,
        String recruitmentTitle,
        Long applicantId,
        String applicantNickname,
        String applicantProfileImageUrl,
        String position,
        String portfolioUrl,
        RecruitmentApplicationStatus status,
        LocalDateTime createdAt,
        LocalDateTime reviewedAt
) {
}
