package cluverse.university.service.response;

import cluverse.university.domain.University;

public record UniversitySummaryResponse(
        Long universityId,
        String universityName,
        String universityBadgeImageUrl
) {
    public static UniversitySummaryResponse from(University university) {
        return new UniversitySummaryResponse(
                university.getId(),
                university.getName(),
                university.getBadgeImageUrl()
        );
    }
}
