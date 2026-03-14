package cluverse.university.service.response;

import cluverse.university.domain.University;

public record UniversityDetailResponse(
        Long universityId,
        String universityName,
        String emailDomain,
        String universityBadgeImageUrl,
        String address,
        boolean isActive
) {
    public static UniversityDetailResponse from(University university) {
        return new UniversityDetailResponse(
                university.getId(),
                university.getName(),
                university.getEmailDomain(),
                university.getBadgeImageUrl(),
                university.getAddress(),
                university.isActive()
        );
    }
}
