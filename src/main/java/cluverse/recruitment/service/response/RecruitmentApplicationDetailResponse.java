package cluverse.recruitment.service.response;

import cluverse.recruitment.domain.RecruitmentApplicationStatus;

import java.time.LocalDateTime;
import java.util.List;

public record RecruitmentApplicationDetailResponse(
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
        Long reviewedBy,
        String reviewerNickname,
        LocalDateTime reviewedAt,
        String latestReviewNote,
        List<RecruitmentApplicationAnswerResponse> answers,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public RecruitmentApplicationDetailResponse {
        answers = answers == null ? List.of() : List.copyOf(answers);
    }
}
