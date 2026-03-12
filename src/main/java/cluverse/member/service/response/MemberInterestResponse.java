package cluverse.member.service.response;

public record MemberInterestResponse(
        Long interestId
) {
    public static MemberInterestResponse from(Long interestId) {
        return new MemberInterestResponse(interestId);
    }
}
