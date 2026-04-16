package cluverse.member.service.response;

public record StudentVerificationUniversityResponse(
        Long universityId,
        String universityName,
        String emailDomain
) {
}
