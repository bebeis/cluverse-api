package cluverse.university.service.response;

public record UniversitySummaryResponse(
        Long universityId,
        String universityName,
        String universityBadgeImageUrl
) {
}
