package cluverse.member.service.response;

public record MemberNicknameAvailabilityResponse(
        String nickname,
        boolean available
) {
}
