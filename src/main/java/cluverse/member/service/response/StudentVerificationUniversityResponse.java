package cluverse.member.service.response;

import cluverse.university.domain.University;

public record StudentVerificationUniversityResponse(
        Long universityId,
        String universityName,
        String emailDomain
) {

    public static StudentVerificationUniversityResponse from(University university) {
        if (university == null) {
            return null;
        }

        return new StudentVerificationUniversityResponse(
                university.getId(),
                university.getName(),
                university.getEmailDomain()
        );
    }
}
