package cluverse.university.service.response;

public record UniversityDetailResponse(
        Long universityId,
        String universityName,
        String emailDomain,
        String universityBadgeImageUrl,
        String address,
        boolean isActive
) {
}
